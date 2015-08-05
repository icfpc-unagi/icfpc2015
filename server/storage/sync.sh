#!/bin/bash

cd "$(dirname "${BASH_SOURCE}")"
for directory in src data bin example java; do
  if [ -d ~/"github/${directory}/" ]; then
    rsync -a --delete --exclude='.git' --delete-excluded \
        ~/"github/${directory}/" ~/"Dropbox/ICFPC2015/github/${directory}/"
  else
    if [ -d ~/"Dropbox/ICFPC2015/github/${directory}/" ]; then
      rm -rf ~/"Dropbox/ICFPC2015/github/${directory}/"
    fi
  fi
done
