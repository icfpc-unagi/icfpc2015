#!/bin/bash

source imosh

cd /alloc/dropbox/www/results

for file in */*.json; do
  if [ ! -f "../scores/${file%.json}.txt" ]; then
    mkdir -p "$(dirname "../scores/${file}")"
    if /alloc/global/bin/sim_main \
        --output_score \
        "/mirror/github/data/problems/${file##*/}" \
        "${file}" > "../scores/${file%.json}.txt.bak"; then
      mv "../scores/${file%.json}.txt.bak" "../scores/${file%.json}.txt"
    else
      rm "../scores/${file%.json}.txt.bak"
      printf -1 > "../scores/${file%.json}.txt"
    fi
  fi
done
