<?php

require_once(dirname(__FILE__) . '/stream.php');

function Solve($data, $phrases, $end_time) {
  return [
      'problemId' => $data['id'],
      'seed' => $data['sourceSeeds'][0],
      'tag' => '-',
      'solution' => 'Ei! Ei!'];
}
