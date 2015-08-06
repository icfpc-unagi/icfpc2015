<?php

$CONFIG = [
    'worker.stderr' => '1',
    'controller.debug' => '0',
];

class Stream {
  public function __construct($command) {
    global $CONFIG;
    $GLOBALS['STREAM_ID'] = -1;
    if (is_null($command)) {
      $this->stdin = STDOUT;
      $this->stdout = STDIN;
      $this->stderr = STDERR;
      return;
    }
    $spec = [['pipe', 'r'], ['pipe', 'w']];
    if (boolval($CONFIG['worker.stderr'])) {
      $spec[] = STDERR;
    } else {
      $spec[] = ['file', '/dev/null', 'w'];
    }
    $this->process = proc_open($command, $spec, $pipes);
    if ($this->process === FALSE) {
      trigger_error("Failed to run command: $command");
      $this->process = NULL;
      return;
    }
    foreach ($pipes as $pipe) {
      stream_set_blocking($pipe, FALSE);
    }
    $this->stdin = $pipes[0];
    $this->stdout = $pipes[1];
    $this->stderr = STDERR;
  }

  public function __destruct() {
    $this->Close();
  }

  public function KillProcesses($pid) {
    posix_kill($pid, @constant('SIGSTOP') ?: 17);
    $output = [];
    exec('ps -axo ppid,pid', $output);
    $pids = [$pid];
    foreach ($output as $line) {
      $line = explode(' ', trim(preg_replace('%\s+%', ' ', $line)), 2);
      if (count($line) != 2) continue;
      if (intval($line[0]) == $pid) {
        $this->KillProcesses(intval($line[1]));
      }
    }
    posix_kill($pid, @constant('SIGTERM') ?: 15);
    posix_kill($pid, @constant('SIGCONT') ?: 19);
  }

  public function Kill() {
    if (is_null($this->process)) return;
    for ($i = 0; $i < 10; $i++) {
      $status = proc_get_status($this->process);
      if (!$status['running']) return;
      if ($i == 0) {
        $this->KillProcesses($status['pid']);
      }
      usleep(1000 << $i);
    }
    posix_kill($status['pid'], @constant('SIGKILL') ?: 9);
  }

  public function Close() {
    $this->Kill();
    if (!is_null($this->stdin) && $this->stdin != STDOUT) {
      fclose($this->stdin);
      $this->stdin = NULL;
    }
    if (!is_null($this->stdout) && $this->stdout != STDIN) {
      fclose($this->stdout);
      $this->stdout = NULL;
    }
    if (!is_null($this->stderr) && $this->stderr != STDERR) {
      fclose($this->stderr);
      $this->stderr = NULL;
    }
    if (!is_null($this->process)) {
      proc_close($this->process);
      $this->process = NULL;
    }
  }

  public function PrintLine($message) {
    fwrite($this->stdin, rtrim($message, "\r\n") . "\n");
    fflush($this->stdin);
  }

  public $process = NULL;
  public $stdin = NULL;
  public $stdout = NULL;
  public $stderr = NULL;
}

class NineStream {
  public function __destruct() {
    foreach ($this->streams as $stream) {
      $stream->Close();
    }
    $this->streams = [];
  }

  public function Start() {
    global $CONFIG;
    $this->streams = [-2 => new Stream(NULL)];
    if (getenv('NINESTREAM_INIT') !== FALSE) {
      $init_commands = trim(file_get_contents(getenv('NINESTREAM_INIT')));
      foreach (explode("\n", $init_commands) as $command) {
        $this->Command($command);
      }
    }
    while (TRUE) {
      $stdin = $this->streams[-2]->stdout;
      $stdout = $this->streams[-2]->stdin;
      if (@posix_isatty($stdin) && @posix_isatty($stdout)) {
        fwrite(STDOUT, '> ');
        fflush(STDOUT);
      }
      $buffer = '';
      while (TRUE) {
        stream_set_blocking($stdin, TRUE);
        $buffer .= fgets($stdin);
        if (substr($buffer, -1) == "\n" || feof($stdin)) {
          break;
        }
      }
      $buffer = rtrim($buffer, "\r\n");
      if ($stdout != STDOUT && boolval($CONFIG['controller.debug'])) {
        fwrite(STDERR, "> $buffer\n");
        fflush(STDERR);
      }
      if (preg_match('%^exit(?:| (\d+))$%', $buffer, $match)) {
        exit(isset($match[1]) ? intval($match[1]) : 0);
      }
      if ($buffer == '' && feof($stdin)) {
        fwrite(STDERR, "Controller stream exited without calling exit.\n");
        fflush(STDERR);
        exit(1);
      }
      $result = strtr($this->Command($buffer), ["\r" => '', "\n" => '']);
      fwrite($stdout, "$result\n");
      fflush($stdout);
      if (isset($this->streams[-3])) {
        unset($this->streams[-3]);
      }
      if ($stdout != STDOUT && boolval($CONFIG['controller.debug'])) {
        fwrite(STDERR, "$result\n");
        fflush(STDERR);
      }
    }
  }

  public function Command($command) {
    global $CONFIG;
    if (trim($command) == '') {
      return 'BAD_REQUEST Empty command.';
    }
    $args = explode(' ', $command);
    $command = array_shift($args);
    $buffer = '';
    switch (strtolower($command)) {
      case 'help':
        $commands = ['set', 'get', 'config', 'run', 'exec',
                     'stream', 'write', 'writeall', 'close', 'closeall'];
        sort($commands);
        array_unshift($commands, 'OK');
        return implode(' ', $commands);
      case 'set':
        if (count($args) < 2) {
          return 'BAD_REQUEST run requires 2+ arguments, but ' .
                 count($args) . '.';
        }
        $key = array_shift($args);
        $value = implode(' ', $args);
        if (!isset($CONFIG[$key])) {
          return "NOT_FOUND No such a config: $key";
        }
        $CONFIG[$key] = $value;
        return 'OK';
      case 'get':
        if (count($args) != 1) {
          return 'BAD_REQUEST get requires exactly one argument, but ' .
                 count($args) . '.';
        }
        $key = array_shift($args);
        if (!isset($CONFIG[$key])) {
          return "NOT_FOUND No such a config: $key";
        }
        return "OK $CONFIG[$key]";
      case 'config':
        $pattern = '*';
        if (count($args) > 0) {
          $pattern = implode(' ', $args);
        }
        $pattern = '/^' . strtr(preg_quote($pattern, '/'),
                                ['\\*' => '.*', '\\?' => '.']) . '$/';
        $keys = ['OK'];
        foreach (array_keys($CONFIG) as $key) {
          if (preg_match($pattern, $key)) {
            $keys[] = $key;
          }
        }
        return implode(' ', $keys);
      case 'run':
        if (count($args) < 2) {
          return 'BAD_REQUEST run requires 2+ arguments, but ' .
                 count($args) . '.';
        }
        $count = array_shift($args);
        if (!preg_match('%^\d{0,6}$%', $count)) {
          return 'BAD_REQUEST run\'s first argument must be an integer, ' .
                 "but $count.";
        }
        $count = intval($count);
        $stream_command = implode(' ', $args);
        for ($i = 0; $i < $count; $i++) {
          $stream = new Stream($stream_command);
          if (is_null($stream->process)) {
            return "INTERNAL_ERROR Failed to run the command: $stream_command";
          }
          $this->streams[] = $stream;
        }
        return 'OK';
      case 'exec':
        if (count($args) < 1) {
          return 'BAD_REQUEST run requires 1+ arguments, but ' .
                 count($args) . '.';
        }
        if (!isset($this->streams[-1])) {
          $this->streams[-1] = new Stream(NULL);
        }
        $this->streams[-3] = $this->streams[-2];
        $this->streams[-2] = new Stream(implode(' ', $args));
        return 'OK';
      case 'stream':
        $streams = ['OK'];
        foreach (array_keys($this->streams) as $stream_id) {
          if ($stream_id < 0) continue;
          $streams[] = $stream_id;
        }
        return implode(' ', $streams);
      case 'read':
        if (count($args) > 1) {
          return 'BAD_REQUEST read exactly takes 0 or 1 argument.';
        }
        if (count($args) == 0) {
          $timeout = 0;
        } else {
          $timeout = array_shift($args);
          if (!is_numeric($timeout)) {
            return 'BAD_REQUEST timeout must be a number.';
          }
          $timeout = floatval($timeout);
          if ($timeout < 0) $timeout = NULL;
        }
        while (TRUE) {
          $reads = [];
          foreach ($this->streams as $stream_id => $stream) {
            if ($stream_id < -1) continue;
            $reads[] = $stream->stdout;
          }
          if (count($reads) == 0) {
            return 'NOT_FOUND No processes are running.';
          }
          stream_select($reads, $writes = [], $excepts = [], $timeout);
          if (count($reads) == 0) {
            return 'NO_CONTENT';
          }
          $read = array_pop($reads);
          foreach ($this->streams as $id => $stream) {
            if ($read == $stream->stdout) break;
          }
          if (!isset($this->buffers[$id])) {
            $this->buffers[$id] = '';
          }
          $this->buffers[$id] .= fgets($read);
          if (substr($this->buffers[$id], -1) == "\n") break;
          if (feof($read)) {
            if ($buffer == '') {
              unset($this->streams[$id]);
              continue;
            }
            break;
          }
        }
        $buffer = $this->buffers[$id];
        $this->buffers[$id] = '';
        if (feof($read)) {
          unset($this->streams[$id]);
        }
        return "OK $id " . rtrim($buffer, "\r\n");
      case 'write':
        if (count($args) < 2) {
          return 'BAD_REQUEST write requires 2+ arguments, but ' .
                  count($args) . '.';
        }
        $stream_id = array_shift($args);
        if (!preg_match('%^-?\d{0,6}$%', $stream_id)) {
          return 'BAD_REQUEST write\'s first argument must be an integer, ' .
                 "but $stream_id.";
        }
        $stream_id = intval($stream_id);
        $data = implode(' ', $args);
        if (!isset($this->streams[$stream_id])) {
          return "NOT_FOUND No such stream: $stream_id";
        }
        $this->streams[$stream_id]->PrintLine($data);
        return 'OK';
      case 'writeall':
        if (count($args) < 1) {
          return 'BAD_REQUEST writeall requires 1+ arguments.';
        }
        $data = implode(' ', $args);
        foreach ($this->streams as $stream_id => $stream) {
          if ($stream_id < 0) continue;
          $stream->PrintLine($data);
        }
        return 'OK';
      case 'close':
        if (count($args) < 1) {
          return 'BAD_REQUEST close requires exactly 1 argument.';
        }
        $stream_id = array_shift($args);
        if (!preg_match('%^\d{0,6}$%', $stream_id)) {
          return 'BAD_REQUEST close\'s first argument must be an integer, ' .
                 "but $stream_id.";
        }
        $stream_id = intval($stream_id);
        if (!isset($this->streams[$stream_id])) {
          return "NOT_FOUND No such stream: $stream_id";
        }
        $this->streams[$stream_id]->Close();
        unset($this->streams[$stream_id]);
        return 'OK';
      case 'closeall':
        foreach ($this->streams as $stream_id => $stream) {
          if ($stream_id < 0) continue;
          $stream->Close();
          unset($this->streams[$stream_id]);
        }
        return 'OK';
    }
    return "NOT_IMPLEMENTED Not implemented command: $command";
  }

  public $buffers = [];
  public $streams = [];
}

$nine_stream = new NineStream();
$nine_stream->Start();
