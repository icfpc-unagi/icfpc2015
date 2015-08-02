SERVICE := ninemaster

start: check
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
		    --volume=/alloc:/alloc \
		    --volume=/ninecluster/ninemaster:/ninemaster \
		    --publish=2200:22 \
		    --publish=2201-2219:2201-2219 \
		    --publish=22000-22127:22000-22127 \
		    local/$(SERVICE); \
	fi
.PHONY: start

stop: check
	-docker kill $(SERVICE) 2>/dev/null
	-docker rm --force $(SERVICE) 2>/dev/null
.PHONY: stop

restart: check
	make stop
	make start
.PHONY: restart

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
