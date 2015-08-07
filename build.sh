#!/bin/bash

if which bazel >/dev/null 2>/dev/null; then
  BAZEL='bazel'
else
  BAZEL='bazel/output/bazel'
  if [ ! -e "${BAZEL}" ]; then
    wget https://storage.googleapis.com/archive-imoz-jp/Repository/Bazel/bazel-ecd4ec4.tar.xz
    tar -Jxvf bazel-*.tar.xz
    pushd bazel
    ./compile.sh
    popd
  fi
fi

targets=(
    //...
    //src/wata:wata_deploy.jar
)

"${BAZEL}" build "${targets[@]}"
