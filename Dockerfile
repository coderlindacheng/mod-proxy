# This file describes how to build Vert.x mod-proxy into a runnable linux container with all dependencies installed
# To build:
# 1) Install docker (http://docker.io)
# 2) Clone mod-proxy repo if you haven't already: git clone https://github.com/arnos/mod-proxy
# 3) Build: cd mod-proxy && docker build .
# 4) Run:
# docker run -d <imageid>
# redis-cli
#
# VERSION		0.1

from	ubuntu:12.04
run	echo "deb http://archive.ubuntu.com/ubuntu precise main universe" > /etc/apt/sources.list
run	apt-get -y update
run	apt-get -y install wget git redis-server supervisor openjdk-7-jdk
run	wget -O - http://dl.bintray.com/vertx/downloads/vert.x-2.1RC2.tar.gz | tar --directory /usr/local/ --strip-components=1 -xz
run	mkdir -p /var/log/supervisor
add	./supervisord.conf /etc/supervisor/conf.d/supervisord.conf
add	./config/config.json /usr/local/lib/vertx/mod-proxy/config/config.json
expose	80
expose	6379
cmd	["supervisord", "-n"]
