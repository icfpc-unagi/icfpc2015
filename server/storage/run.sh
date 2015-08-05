#!/bin/bash

source "$(dirname "${BASH_SOURCE}")/../../bin/imos-variables" || exit 1
eval "${IMOSH_INIT}"

exec {LOCK}>>"$(dirname "${BASH_SOURCE}")/lock"
if ! flock -w 1 "${LOCK}"; then
  echo 'This script is already running, so exiting...' >&2
  exit
fi

while :; do
  {
    echo 'HTTP/1.0 200 OK'
    echo 'Content-Type: text/html'
    echo
    echo 'OK'
  } | nc -l 18080
  pushd ~/github
  git pull
  popd
  bash "$(dirname "${BASH_SOURCE}")/sync.sh"
done &

~/.dropbox-dist/dropboxd

sub::exit
