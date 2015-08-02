#!/bin/bash

mkdir -p /alloc/dropbox /alloc/global /alloc/github
/bin/fusermount -uz /alloc/dropbox
/bin/fusermount -uz /alloc/global
/bin/fusermount -uz /alloc/github
/usr/bin/sshfs -o allow_other,uid=65534,gid=65534 ninetan@storage.icfpc.imoz.jp:/dropbox /alloc/dropbox
/usr/bin/sshfs -o allow_other,uid=65534,gid=65534 ninetan@storage.icfpc.imoz.jp:/alloc /alloc/global
/usr/bin/sshfs -o allow_other,ro,uid=65534,gid=65534 ninetan@storage.icfpc.imoz.jp:github /alloc/github
cd /github/server/ninecluster
while :; do
  if ! make start; then
    sleep 1
  fi
done
