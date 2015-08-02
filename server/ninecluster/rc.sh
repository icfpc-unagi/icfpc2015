#!/bin/bash

cd /github
git pull
/usr/bin/screen -dmS ninecluster /bin/bash /github/server/ninecluster/init.sh
