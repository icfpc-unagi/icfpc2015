#!/bin/bash

start_docker() {
  local port="${1}"; shift

  while :; do
    docker run \
        --rm \
        --privileged \
        --publish="${port}:22" \
        --volume=/alloc:/alloc \
        --volume=/ninemaster/tmp:/var/tmp \
        --volume=/ninemaster/config:/usr/imos/config \
        local/ninelet "$@"
    sleep 1
  done
}

cd "$(dirname $0)"

date > dummy
docker build --tag=local/ninelet .

mkdir -p /ninemaster/tmp
chmod ugo=rwxrwxrwt /ninemaster/tmp
mkdir -p /ninemaster/config

for port in $(seq 2201 2209); do
  start_docker "${port}" &
done

for port in $(seq 2210 2219); do
  start_docker "${port}" -d &
done
