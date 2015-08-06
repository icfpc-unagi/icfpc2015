#!/bin/bash

start_docker() {
  local port="${1}"; shift

  while :; do
    docker run \
        --rm \
        --privileged \
        --publish="${port}" \
        --volume=/alloc:/alloc \
        --volume=/mirror:/mirror:ro \
        --volume=/mirror/dropbox/www:/var/www/html:ro \
        --volume=/ninemaster/tmp:/var/tmp \
        --volume=/ninemaster/config:/usr/imos/config \
        local/ninelet /bin/bash -c "$*"
    sleep 1
  done
}

start_ssh_docker() {
  local port="${1}"; shift

  start_docker "${port}:22" \
      "/usr/sbin/sshd -D \\
          -o 'ClientAliveInterval 5' \\
          -o 'ClientAliveCountMax 3' $*"
}

cd "$(dirname $0)"

date > dummy
docker build --tag=local/ninelet .

mkdir -p /ninemaster/tmp
chmod ugo=rwxrwxrwt /ninemaster/tmp
mkdir -p /ninemaster/config

for port in $(seq 2201 2209); do
  start_ssh_docker "${port}" &
done

for port in $(seq 22000 22127); do
  start_ssh_docker "${port}" -d &
done
