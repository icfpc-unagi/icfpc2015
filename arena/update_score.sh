#!/bin/bash

source imosh

cd /alloc/dropbox/www/results

for file in */*.json; do
  if [ ! -f "../scores/${file}" ]; then
    mkdir -p "$(dirname "../scores/${file}")"
    /alloc/global/bin/sim_main "${file}" "../scores/${file}"
  fi
done
