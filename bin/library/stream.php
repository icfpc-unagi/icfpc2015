<?php

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
