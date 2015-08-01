#!/bin/bash

source "$(dirname "${BASH_SOURCE}")/../../bin/imos-variables" || exit 1
eval "${IMOSH_INIT}"

exec {LOCK}>>"$(dirname "${BASH_SOURCE}")/lock"
if ! flock -w 1 "${LOCK}"; then
  echo 'This script is already running, so exiting...' >&2
  exit
fi

bash "$(dirname "${BASH_SOURCE}")/sync.sh" &
~/.dropbox-dist/dropboxd
sub::exit
