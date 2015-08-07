<?php

$files = array_merge(
    glob('*/*/*/*/*/*/*/*/*/*.hpp'),
    glob('*/*/*/*/*/*/*/*/*.hpp'),
    glob('*/*/*/*/*/*/*/*.hpp'),
    glob('*/*/*/*/*/*/*.hpp'),
    glob('*/*/*/*/*/*.hpp'),
    glob('*/*/*/*/*.hpp'),
    glob('*/*/*/*.hpp'),
    glob('*/*/*.hpp'),
    glob('*/*.hpp'),
    glob('*.hpp'));

foreach ($files as $file) {
  $data = file_get_contents($file);
  file_put_contents(
      $file,
      preg_replace('%<boost/([^>\n]*)>%', '"boost/\\1"', $data));
}
