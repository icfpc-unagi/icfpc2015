#!/bin/bash

cd ~/github
while :; do
  {
    echo 'HTTP/1.0 200 OK'
    echo 'Content-Type: text/html'
    echo
    echo 'OK'
  } | nc -l 8080
  git pull
  rsync -a --delete --exclude='.git' --delete-excluded ~/github/ ~/Dropbox/ICFPC2015/github/
done
