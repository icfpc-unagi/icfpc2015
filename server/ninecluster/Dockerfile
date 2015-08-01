FROM ubuntu:14.04
MAINTAINER imos@docker.com

# Let's start with some basic stuff.
RUN apt-get update -qq && apt-get install -qqy \
    apt-transport-https \
    ca-certificates \
    curl \
    lxc \
    iptables

# Install Docker from Docker Inc. repositories.
RUN curl -sSL https://get.docker.com/ubuntu/ | sh

RUN apt-get update -qq & \
    apt-get install -qqy openssh-server
RUN mkdir /var/run/sshd

RUN useradd \
        --home-dir=/home/ninemaster \
        --create-home \
        --uid=20601 \
        --user-group \
        --shell=/bin/bash \
        ninemaster
RUN mkdir -p /home/ninemaster/.ssh
RUN echo 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCrHaL3kdZ2RekCdpkie3fsiv2yVyyWRBOO6Q68Kr+tFStRqtF8q1/UoeteUIOxzwKaAmHoaM9PkItdMBki0BLQDimCZwjjbkosritGDMTJXGd21O72mWaTv+nfq+/ishCdt6gdBYXTejvpPJhq8ZMYhTYJZkWqlGO2CKrWcnHHu1HhnValeqNWS5nh8BULOTMKaixjdzXIkWgm8HyiewvqjZXC3tZlfFDErRpiS7SYfJHd4PujjFCNyiVxZ5yOvEGMXQa1UFxQlfX8H+lAr6qObK50osAdUbvjjbhIhMvZT2higSNNtv/yiaLRnLbbOHomObvqxob5TUVdCkazXX3N imos@docker' > /home/ninemaster/.ssh/authorized_keys
RUN echo 'ninemaster ALL=(ALL:ALL) NOPASSWD: ALL' > /etc/sudoers.d/ninemaster

# Install Ninelet.
ADD ./ninelet /usr/local/ninelet

# Install the magic wrapper.
ADD ./run.sh /usr/local/bin/run.sh
RUN chmod +x /usr/local/bin/run.sh

# Define additional metadata for our image.
VOLUME /var/lib/docker

CMD ["run.sh"]
