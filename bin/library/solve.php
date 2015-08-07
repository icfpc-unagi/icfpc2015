<?php

require_once(dirname(__FILE__) . '/stream.php');

function Solve($data, $phrases, $end_time) {
  $tmp_file = tempnam('/tmp', 'icfpc-data-');
  $data['phrases'] = $phrases;
  file_put_contents($tmp_file, json_encode($data));
  $streams = [];
  foreach ($GLOBALS['PROGRAMS'] as $program_id => $program) {
    $streams[$program_id] = new Stream($program . ' ' . $tmp_file);
  }
  $stream_ids = [];
  foreach ($streams as $stream_id => $stream) {
    $stream_ids[$stream->stdout] = $stream_id;
  }
  $buffers = [];

  $best_score = 0;
  $best_solution = '';
  $best_tag = '';

  $dummy_counter = 0;

  while (TRUE) {
    $reads = [];
    foreach ($streams as $stream) {
      $reads[] = $stream->stdout;
    }
    if (count($reads) == 0) { break; }
    $current_time = array_sum(array_map('floatval', explode(' ', microtime())));
    stream_select($reads, $writes = [], $excepts = [],
                  $end_time - $current_time);
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

    $solution = json_decode($buffer, TRUE);
    if (!isset($solution['solution']) ||
        $solution['solution'] == '') { continue; }
    // TODO(imos): Evaluate the solution and set its score.
    $score = ++$dummy_counter;

    if ($score > $best_score) {
      $best_score = $score;
      $best_solution = $solution['solution'];
      $best_tag = $solution['tag'];
      fwrite(STDERR, "$stream_id: $best_score\n");
    }
  }

  return [
      'problemId' => $data['id'],
      'seed' => $data['sourceSeeds'][0],
      'tag' => $best_tag,
      'solution' => $best_solution];
}
