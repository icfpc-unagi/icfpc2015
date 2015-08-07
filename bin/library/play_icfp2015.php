<?php

date_default_timezone_set('Asia/Tokyo');

$START_TIME = array_sum(array_map('floatval', explode(' ', microtime())));
$TIME_LIMIT = 24 * 3600;
$MEMORY_LIMIT = 0;
$FILES = [];
$PHRASES = [];
$PROGRAMS = [];

$mode = '';
for ($i = 0; $i < count($argv); $i++) {
  switch ($argv[$i]) {
    case '-f':
    case '-t':
    case '-m':
    case '-p':
    case '-x':
      $mode = $argv[$i];
      break;
    default:
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
        case '-x':
          $PROGRAMS[] = $argv[$i];
          break;
      }
      break;
  }
}

if (count($PROGRAMS) == 0) {
  $PROGRAMS = array_map(
      'trim',
      explode("\n", trim(
          file_get_contents(dirname(__FILE__) . '/programs.txt'))));
}

fwrite(STDERR, 'Start time: ' . date('r', $START_TIME) . "\n");
fwrite(STDERR, 'File: ' . json_encode($FILES) . "\n");
fwrite(STDERR, 'Phrases of power: ' . json_encode($PHRASES) . "\n");
fprintf(STDERR, "Time limit: %.3f\n", $TIME_LIMIT);
fprintf(STDERR, "Memory limit: %.3f\n", $MEMORY_LIMIT);
fwrite(STDERR, 'Programs: ' . json_encode($PROGRAMS) . "\n");

require_once(dirname(__FILE__) . '/solve.php');

// TODO(imos): Sort files by difficulty (easy to difficult).

$END_TIME = $START_TIME + $TIME_LIMIT;

$outputs = [];
while (count($FILES) > 0) {
  $current_time = array_sum(array_map('floatval', explode(' ', microtime())));
  $file_end_time = ($END_TIME - $current_time) / count($FILES) + $current_time;
  $file = array_shift($FILES);
  fwrite(STDERR, str_repeat('=', 80) . "\n");
  fwrite(STDERR, "File: $file\nEnd time: " . date('r', $file_end_time) . "\n");
  $data = json_decode(file_get_contents($file), TRUE);
  $seeds = $data['sourceSeeds'];
  while (count($seeds) > 0) {
    $current_time = array_sum(array_map('floatval', explode(' ', microtime())));
    $seed_end_time =
        ($file_end_time - $current_time) / count($seeds) + $current_time;
    $seed = array_shift($seeds);
    fwrite(STDERR, str_repeat('-', 80) . "\n");
    fwrite(STDERR, "Seed: $seed\n");
    fwrite(STDERR, "End time: " . date('r', $seed_end_time) . "\n");
    $data['sourceSeeds'] = [$seed];
    $output = Solve($data, $PHRASES, $seed_end_time);
    if ($output != NULL) {
      $outputs[] = [
          'problemId' => $output['problemId'],
          'seed' => $seed,
          'tag' => $output['tag'],
          'solution' => $output['solution']];
    }
  }
}

echo json_encode($outputs) . "\n";
