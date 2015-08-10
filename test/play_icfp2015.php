<?php

error_reporting(E_ALL);
date_default_timezone_set('Asia/Tokyo');

$START_TIME = array_sum(array_map('floatval', explode(' ', microtime())));
$TIME_LIMIT = 24 * 3600;
$MEMORY_LIMIT = 0;
$FILES = [];
$PHRASES = [];
$PROGRAMS = [];
$STDERR = FALSE;
$VERBOSE = 0;
$SIMULATOR = 'bazel-bin/src/sim/sim_main';
$RECIPE = 'play_icfp2015.txt';
$CORES = 8;

$mode = '';
for ($i = 1; $i < count($argv); $i++) {
  if (preg_match('%^-@?[a-zA-Z]$%', $argv[$i])) {
    $mode = $argv[$i];
    switch ($mode) {
      case '-@e':
        $STDERR = TRUE;
        break;
      case '-@v':
        $VERBOSE = 1;
        break;
    }
  } else {
    switch ($mode) {
      case '-f':
        $FILES[] = $argv[$i];
        break;
      case '-t':
        $TIME_LIMIT = floatval($argv[$i]);
        break;
      case '-m':
        $MEMORY_LIMIT = floatval($argv[$i]);
        break;
      case '-p':
        $PHRASES[] = $argv[$i];
        break;
      case '-c':
        $CORES = intval($argv[$i]);
        break;
      case '-@x':
        $PROGRAMS[] = $argv[$i];
        break;
      case '-@v':
        $VERBOSE = intval($argv[$i]);
        break;
      case '-@s':
        $SIMULATOR = $argv[$i];
        break;
      case '-@r':
        $RECIPE = $argv[$i];
        break;
      default:
        fwrite(STDERR, "Flag option $mode is not supported, so ignored.\n");
    }
  }
}

if (count($PROGRAMS) == 0) {
  $PROGRAMS =
      array_map('trim', explode("\n", trim(file_get_contents($RECIPE))));
}

$PHRASES = array_map('strtolower', $PHRASES);

fwrite(STDERR, 'Start time: ' . date('r', $START_TIME) . "\n");
fwrite(STDERR, 'File: ' . json_encode($FILES) . "\n");
fwrite(STDERR, 'Phrases of power: ' . json_encode($PHRASES) . "\n");
fprintf(STDERR, "Time limit: %.3f\n", $TIME_LIMIT);
fprintf(STDERR, "Memory limit: %.3f\n", $MEMORY_LIMIT);
fwrite(STDERR, "Cores: $CORES\n");
fwrite(STDERR, 'Recipe: ' . $RECIPE . "\n");
fwrite(STDERR, 'Programs: ' . json_encode($PROGRAMS) . "\n");
fwrite(STDERR, 'Worker STDERR: ' . ($STDERR ? 'Yes' : 'No') . "\n");
fwrite(STDERR, 'Verbosity: ' . $VERBOSE . "\n");
fwrite(STDERR, 'Simulator: ' . $SIMULATOR . "\n");

$CONFIG['worker.stderr'] = $STDERR;
// TODO(imos): Sort files by difficulty (easy to difficult).
$END_TIME = $START_TIME + $TIME_LIMIT;

function Logging($color, $message) {
  fwrite(STDERR,
         $color . date('Y-m-d H:i:s] ') . rtrim($message) . "\033[0m\n");
}

function INFO($message) {
  Logging("\033[0;34mI", $message);
}

function WARNING($message) {
  Logging("\033[0;31mW", $message);
}

function GetMicrotime() {
  return array_sum(array_map('floatval', explode(' ', microtime())));
}

function GetTemporaryFile() {
  return tempnam(sys_get_temp_dir(), 'icfpc-data-');
}

class Stream {
  public function __construct($command) {
    $this->command = $command;
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

function GetScore($input_file, $phrases_file, $solution) {
  static $printed = FALSE;
  $tmpfile = GetTemporaryFile();
  file_put_contents($tmpfile, json_encode([$solution]));
  $command = $GLOBALS['SIMULATOR'] . ' ' .
             '--output_score ' .
             '--phrases_of_power=' . $phrases_file . ' ' .
             '--verbose=0 ' .
             $input_file . ' ' . $tmpfile;
  if (!$printed) {
    INFO("Scoring: $command");
    $printed = TRUE;
  }
  $output = trim(exec($command));
  unlink($tmpfile);
  if (is_numeric($output)) {
    return intval($output);
  }
  return NULL;
}

function ReadStreams(
    $streams, $problem_id, $input_file, $phrases_file, $end_time) {
  $buffers = [];

  $best_score = [];
  $best_solution = [];
  $best_tag = [];
  $best_stream = [];

  while (TRUE) {
    $reads = [];
    foreach ($streams as $stream) {
      $reads[] = $stream->stdout;
    }
    if (count($reads) == 0) { break; }
    $current_time = array_sum(array_map('floatval', explode(' ', microtime())));
    $time_limit = $end_time - $current_time;
    if ($time_limit <= 0) {
      WARNING("Time limit exceeded.");
      break;
    }
    $writes = [];
    $excepts = [];
    stream_select($reads, $writes, $excepts, $end_time - $current_time);
    if (count($reads) == 0) { break; }
    $read = array_shift($reads);
    foreach ($streams as $stream_id => $stream) {
      if ($read == $stream->stdout) break;
    }
    if (!isset($buffers[$stream_id])) {
      $buffers[$stream_id] = '';
    }
    $buffers[$stream_id] .= fgets($read);
    if (substr($buffers[$stream_id], -1) != "\n" && !feof($read)) { continue; }
    if (feof($read)) {
      unset($streams[$stream_id]);
    }
    $buffer = $buffers[$stream_id];
    $buffers[$stream_id] = '';
    if (trim($buffer) == '') { continue; }

    if ($GLOBALS['VERBOSE'] >= 1) {
      INFO("$stream_id: solution: $buffer");
    }

    $solutions = json_decode($buffer, TRUE);
    if (!is_array($solutions)) {
      $solutions = [$solutions];
    }
    foreach ($solutions as $solution) {
      if (!isset($solution['solution'])) {
        WARNING("STREAM$stream_id: returns no solution: " . trim($buffer));
        continue;
      }
      if ($solution['solution'] == '') {
        WARNING("STREAM$stream_id: returns an empty solution.");
        continue;
      }
      if (!isset($solution['problemId'])) {
        WARNING("STREAM$stream_id: problemId field is missing.");
        continue;
      }
      if ($solution['problemId'] != $problem_id) {
        WARNING("STREAM$stream_id: problemId is wrong: " .
                "{$problem_id} vs {$solution['problemId']}.");
        continue;
      }
      if (!isset($solution['seed'])) {
        WARNING("STREAM$stream_id: seed field is missing.");
        continue;
      }
      $seed = $solution['seed'];
      $score = GetScore($input_file, $phrases_file, $solution);
      if ($score == NULL) {
        INFO("STREAM$stream_id: invalid output: " . json_encode($solution));
        continue;
      }
      INFO("STREAM$stream_id: evaluated score is $score.");

      if (!isset($best_score[$seed]) || $score > $best_score[$seed]) {
        $best_score[$seed] = $score;
        $best_solution[$seed] = $solution['solution'];
        $best_tag[$seed] = $solution['tag'];
        $best_stream[$seed] = $stream_id;
        INFO("STREAM$stream_id: records $score.");
      }
    }
  }
  $results = [];
  foreach ($best_solution as $seed => $solution) {
    $results["$problem_id:$seed"] = [
        'problemId' => $problem_id,
        'seed' => $seed,
        'tag' => $best_tag[$seed],
        'solution' => $solution,
        'stream' => $best_stream[$seed]];
  }
  return $results;
}

function CreateCommand($program, $input_file, $phrases, $end_time) {
  $args = [];
  foreach ($phrases as $phrase) {
    $args[] = '-p';
    $args[] = $phrase;
  }
  $args[] = '-f';
  $args[] = $input_file;
  $args[] = '-t';
  $current_time = array_sum(array_map('floatval', explode(' ', microtime())));
  $args[] = intval(ceil($end_time - $current_time));
  return $program . ' ' . implode(' ', array_map('escapeshellarg', $args));
}

class Game {
  private function SolveProblem($problem, $phrase_file, $end_time) {
    if (GetMicrotime() > $end_time) return [];

    $unlink_files = [];
    $results = [];

    $seeds = array_values(array_unique($problem['sourceSeeds']));
    if (count($seeds) < 1) {
      WARNING("Problem {$problem['id']} has no seeds.");
      return [];
    }
    $seed_files = [];
    foreach ($seeds as $seed) {
      $file = GetTemporaryFile();
      $seed_files[$seed] = $file;
      $unlink_files[] = $file;
      $simplified_problem = $problem;
      $simplified_problem['sourceSeeds'] = [$seed];
      file_put_contents($file, json_encode($simplified_problem));
    }

    if (count($GLOBALS['PROGRAMS']) == 0) {
      WARNING("No programs are specified.");
      return [];
    }

    $seed_index = 0;
    if (FALSE && count($GLOBALS['PROGRAMS']) == 1) {
      $program_ids = array_keys($GLOBALS['PROGRAMS']);
      $program_id = $program_ids[0];
    } else {
      for (; $seed_index < count($seeds); $seed_index++) {
        $streams = [];
        $seed = $seeds[$seed_index];
        INFO("SEED{$seed}: #${seed_index}");
        if (count($seeds) == 1) {
          $seed_end_time = $end_time;
        } else {
          $current_time = GetMicrotime();
          // TODO(imos): Calculate porition precisely.
          $seed_end_time =
              ($end_time - $current_time) / (count($seeds) - $seed_index) +
              $current_time;
        }
        $input_file = $seed_files[$seed];
        foreach ($GLOBALS['PROGRAMS'] as $program_id => $program) {
          $streams[$program_id] =
              new Stream(CreateCommand(
                  $program, $input_file, $GLOBALS['PHRASES'], $seed_end_time));
          INFO("PROGRAM{$program_id}: {$streams[$program_id]->command}");
        }
        $results += ReadStreams(
            $streams, $problem['id'], $input_file, $phrase_file,
            $seed_end_time);
        foreach ($results as $result) {
          if (isset($result['stream'])) {
            $program_id = $result['stream'];
            INFO("PROGRAM{$program_id} is best.");
            break;
          } else {
            WARNING("stream field must be specified.");
          }
        }
      }
    }

    /*
    for (; $seed_index < count($seeds);) {
      if (GetMicrotime() > $end_time) break;

      $seed_count = count($seeds) - $seed_index;
      $seed_block = $seed_count / ceil($seed_count / $GLOBALS['CORES']);
      $seed_border = $seed_index + $seed_block;

      $streams = [];
      $current_time = GetMicrotime();
      $portion = min(ceil(1 + $seed_border - $seed_index) /
                     (count($seeds) - $seed_index), 1);
      $group_end_time = ($end_time - $current_time) * $portion + $current_time;
      INFO("Seed group end time: " . date('r', $group_end_time));
      for (; $seed_index < $seed_border &&
             $seed_index < count($seeds); $seed_index++) {
        $seed = $seeds[$seed_index];
        $input_file = $seed_files[$seed];
        $program = $GLOBALS['PROGRAMS'][$program_id];
        $streams[$seed] =
            new Stream(CreateCommand(
                $program, $input_file, $GLOBALS['PHRASES'], $group_end_time));
        INFO("SEED{$seed}: #{$seed_index}");
        INFO("SEED{$seed}: {$streams[$seed]->command}");
      }
      if (count($streams) > 0) {
        $results += ReadStreams(
            $streams, $problem['id'], $input_file, $phrase_file,
            $group_end_time);
      }
    }
    */

    // Only for checking.
    foreach ($results as $key => $result) {
      if ($result['problemId'] != $problem['id']) {
        WARNING(
            "Invalid problemId: {$result['problemId']} vs {$problem['id']}");
        unset($results[$key]);
        continue;
      }
      if (!in_array($result['seed'], $seeds)) {
        WARNING("Not existing seed: {$result['seed']}");
        unset($results[$key]);
        continue;
      }
    }

    return $results;
  }

  public function SolveProblems($problems, $phrase_file, $end_time) {
    $results = [];

    $prioritized_problems = [];
    foreach ($problems as $key => $problem) {
      $prioritized_problems[
          sprintf('%020d',
                  $problem['height'] * $problem['width'] *
                      count($problem['sourceSeeds']) *
                      $problem['sourceLength']) . ":$key"] = $problem;
    }
    ksort($prioritized_problems);
    foreach ($prioritized_problems as $weight => $problem) {
      $weight = intval($weight);
      INFO("Problem: {$problem['id']} (Weight: $weight)");
    }

    while (count($prioritized_problems) > 0) {
      INFO(str_repeat('=', 80));
      $current_time = GetMicrotime();
      $problem_end_time =
          ($end_time - $current_time) / count($prioritized_problems) +
          $current_time;
      $problem = array_shift($prioritized_problems);
      INFO("Problem ID: {$problem['id']}");
      INFO("Problem end time: " . date('r', $problem_end_time));
      $problem_results =
          $this->SolveProblem($problem, $phrase_file, $problem_end_time);
      if (!is_array($problem_results)) {
        WARNING("{$problem['id']} has no solutions.");
        continue;
      }
      foreach ($problem_results as $result) {
        INFO("Problem result: Problem: {$result['problemId']}, Seed: {$result['seed']}");
        $results[intval($result['problemId'])][intval($result['seed'])] = $result;
      }
    }

    $final_results = [];
    foreach ($problems as $problem) {
      foreach ($problem['sourceSeeds'] as $seed) {
        $problem_id = intval($problem['id']);
        $seed = intval($seed);
        if (isset($results[$problem['id']][$seed])) {
          $result = $results[$problem['id']][$seed];
          $final_results[] = [
              'problemId' => $result['problemId'],
              'seed' => $result['seed'],
              'tag' => $result['tag'],
              'solution' => $result['solution']];
        } else {
          WARNING("<Problem ID: {$problem['id']}, Seed: {$seed}> is missing.");
        }
      }
    }
    return array_values($final_results);
  }
}

function LoadProblems($files) {
  $problems = [];
  foreach ($files as $file) {
    INFO("Loading $file...");
    $problem = json_decode(file_get_contents($file), TRUE);
    $problems[] = $problem;
  }
  return $problems;
}

function Main() {
  global $FILES, $END_TIME, $PHRASES;
  $phrase_file = GetTemporaryFile();
  file_put_contents($phrase_file, implode("\n", $PHRASES));
  $game = new Game();
  echo json_encode($game->SolveProblems(
      LoadProblems($FILES), $phrase_file, $END_TIME)) . "\n";
}

Main();
