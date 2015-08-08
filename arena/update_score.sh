#!/bin/bash

source imosh

cd /alloc/dropbox/www/results

for file in */*.json; do
  if [ ! -f "../scores/${file}" ]; then
    mkdir -p "$(dirname "../scores/${file}")"
    /alloc/global/bin/sim_main \
        --output_score \
        "/mirror/github/data/problems/${file##*/}" \
        "${file}" > "../scores/${file%.json}.txt"
  fi
done
