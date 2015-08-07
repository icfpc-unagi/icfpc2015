#!/bin/bash

TARGET=''
if [ -f "${BASH_SOURCE}.runfiles/ICFPC2015/ICFPC2015/Main.exe" ]; then
  TARGET="${BASH_SOURCE}.runfiles/ICFPC2015/ICFPC2015/Main.exe"
elif [ -f "$(dirname "${BASH_SOURCE}")/Main.exe" ]; then
  TARGET="$(dirname "${BASH_SOURCE}")/Main.exe"
else
  echo 'Main.exe is not found.' >&2
  exit 1
fi
mono "${TARGET}"
