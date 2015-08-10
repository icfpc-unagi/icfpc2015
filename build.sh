#!/bin/bash

sudo apt-get -qqy update
sudo apt-get -qqy install \
    build-essential devscripts ubuntu-standard software-properties-common \
    screen lxc traceroute gdb \
    vim git subversion mercurial cmake make \
    dos2unix nkf curl xz-utils \
    libarchive-dev pkg-config zip g++ zlib1g-dev \
    php5-cli mono-devel
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 \
    select true | sudo debconf-set-selections && \
    sudo add-apt-repository -y ppa:webupd8team/java && \
    sudo apt-get update -qq && sudo apt-get install -qqy oracle-java8-installer

if which bazel >/dev/null 2>/dev/null; then
  BAZEL='bazel'
else
  BAZEL='../bazel/output/bazel'
  if [ ! -e "${BAZEL}" ]; then
    pushd ~
    wget https://storage.googleapis.com/archive-imoz-jp/Repository/Bazel/bazel-ecd4ec4.tar.xz
    tar -Jxvf bazel-*.tar.xz
    cd bazel
    ./compile.sh
    popd
  fi
fi

targets=(
    //src/sim:sim_main
)

"${BAZEL}" build "${targets[@]}"
