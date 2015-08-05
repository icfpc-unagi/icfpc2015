#!/bin/bash

sync_cloud() {
  rsync -va --delete --delete-excluded \
      ninetan@storage.icfpc.imoz.jp:Dropbox/ICFPC2015/ /mirror/dropbox/
  rsync -va --delete --delete-excluded \
      ninetan@storage.icfpc.imoz.jp:/alloc/ /mirror/global/
  rsync -va --delete --delete-excluded \
      ninetan@storage.icfpc.imoz.jp:github/ /mirror/github/
}

mkdir -p /alloc/dropbox /alloc/global /alloc/github
/bin/fusermount -uz /alloc/dropbox
/bin/fusermount -uz /alloc/global
/bin/fusermount -uz /alloc/github
/usr/bin/sshfs -o allow_other,uid=65534,gid=65534 ninetan@storage.icfpc.imoz.jp:Dropbox/ICFPC2015 /alloc/dropbox
/usr/bin/sshfs -o allow_other,uid=65534,gid=65534 ninetan@storage.icfpc.imoz.jp:/alloc /alloc/global
/usr/bin/sshfs -o allow_other,ro,uid=65534,gid=65534 ninetan@storage.icfpc.imoz.jp:github /alloc/github

mkdir -p /mirror/dropbox /mirror/global /mirror/github
sync_cloud

while :; do
  if [ "$(dig +short dev.icfpc.imoz.jp)" == \
       "$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)" ]; then
    sleep 1
  else
    sleep 10
  fi
  sync_cloud
done &

cd /github/server/ninecluster
while :; do
  if ! make start; then
    sleep 1
  fi
done
