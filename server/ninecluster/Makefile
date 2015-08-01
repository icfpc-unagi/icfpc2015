SERVICE := ninemaster
DISK ?= 50G
ALLOC_DISK ?= 10G

start: check
	make mount
	if ! docker top $(SERVICE) >/dev/null 2>/dev/null; then \
		set -e -x; \
		mkdir -p /ninecluster/alloc; \
		mkdir -p /ninecluster/ninemaster; \
		docker build --tag=local/$(SERVICE) .; \
		docker rm --force $(SERVICE) 2>/dev/null || true; \
		docker run \
		    --privileged \
		    --name=$(SERVICE) \
		    --hostname=$(SERVICE) \
		    --volume=/ninecluster/alloc:/alloc \
		    --volume=/ninecluster/ninemaster:/ninemaster \
		    --publish=2200:22 \
		    --publish=2201-2219:2201-2219 \
		    local/$(SERVICE); \
	fi
.PHONY: start

stop: check
	-docker kill $(SERVICE) 2>/dev/null
	-docker rm --force $(SERVICE) 2>/dev/null
	-make unmount
.PHONY: stop

restart: check
	make stop
	make start
.PHONY: restart

mount: check
	if ! mountpoint -q /ninecluster/ninemaster; then \
	  set -e -x; \
	  mkdir -p /ninecluster/ninemaster; \
	  if [ ! -f /ninecluster/ninemaster.dmg ]; then \
	    dd if=/dev/zero of=/ninecluster/ninemaster.dmg count=1 bs=1M seek=10000; \
	    mkfs.ext4 /ninecluster/ninemaster.dmg; \
	  fi; \
	  e2fsck -y -f /ninecluster/ninemaster.dmg; \
	  resize2fs /ninecluster/ninemaster.dmg $(DISK); \
	  mount -t auto -o loop /ninecluster/ninemaster.dmg /ninecluster/ninemaster; \
	fi
.PHONY: mount

unmount: check
	-fuser --kill /ninecluster/ninemaster 2>/dev/null
	-umount -f /ninecluster/ninemaster 2>/dev/null
.PHONY: unmount

defrag: check
	-e4defrag -c /ninecluster/ninemaster.dmg
	-e2fsck -y -f /ninecluster/ninemaster.dmg
	-resize2fs -M /ninecluster/ninemaster.dmg
.PHONY: defrag

backup: check
	xz --stdout --compress /ninecluster/ninemaster.dmg > /ninecluster/ninemaster.dmg.xz
.PHONY: backup

install:
	test "$$(whoami)" = 'root'
	apt-get update -qq
	apt-get install -qqy apt-transport-https ca-certificates curl lxc iptables
	curl -sSL https://get.docker.com/ubuntu/ | sh
	sed -i 's/^GRUB_CMDLINE_LINUX=".*"/GRUB_CMDLINE_LINUX="cgroup_enable=memory swapaccount=1"/' /etc/default/grub
	update-grub
.PHONY: install

check:
	test "$$(whoami)" = 'root'
.PHONY: check
