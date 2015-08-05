#!/bin/bash

cd ~/github
while :; do
  {
    echo 'HTTP/1.0 200 OK'
    echo 'Content-Type: text/html'
    echo
    echo 'OK'
  } | nc -l 18080
  git pull
  for directory in src data bin; do
    rsync -a --delete --exclude='.git' --delete-excluded \
        ~/"github/${directory}/" ~/"Dropbox/ICFPC2015/github/${directory}/"
  done
done
