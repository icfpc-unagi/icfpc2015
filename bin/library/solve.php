<?php

require_once(dirname(__FILE__) . '/stream.php');

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

function GetScore($input_file, $phrases_file, $solution) {
  $command =
      'echo \'[' .
      str_replace("'", "'\\''", json_encode($solution)) . ']\' | ' .
      dirname(__FILE__) . '/../../bazel-bin/src/sim/sim_main ' .
      '--output_score ' .
      '--phrases_of_power=' . $phrases_file . ' ' .
      '--verbose=0 ' .
      $input_file . ' ' .
      '/dev/stdin';
  $output = exec($command);
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
        WARNING("STREAM$stream_id: returns no solution.");
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
        INFO("STREAM$stream_id: invalid output: $seed => $solution");
      }
      INFO("STREAM$stream_id: evaluated score is $score.");

      if (!isset($best_score[$seed]) || $score > $best_score) {
        $best_score[$seed] = $score;
        $best_solution[$seed] = $solution['solution'];
        $best_tag[$seed] = $solution['tag'];
        $best_stream[$seed] = $stream_id;
        INFO("STREAM$stream_id: records $score!");
      }
    }
  }
  $results = [];
  foreach ($best_solution as $seed => $solution) {
    $results[] = [
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

function Solve($data, $phrases, $end_time) {
  $unlink_files = [];

  $input_file = tempnam(sys_get_temp_dir(), 'icfpc-data-');
  $unlink_files[] = $input_file;
  $data['phrases'] = $phrases;
  file_put_contents($input_file, json_encode($data));

  $phrases_file = tempnam(sys_get_temp_dir(), 'icfpc-data-');
  $unlink_files[] = $phrases_file;
  file_put_contents($phrases_file, implode("\n", $phrases));

  $streams = [];
  foreach ($GLOBALS['PROGRAMS'] as $program_id => $program) {
    $streams[$program_id] =
        new Stream(CreateCommand($program, $input_file, $phrases, $end_time));
  }
  $results = ReadStreams(
      $streams, $data['id'], $input_file, $phrases_file, $end_time);

  foreach ($unlink_files as $file) {
    unlink($file);
  }

  return $results;
}
