#!/bin/zsh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"
# Docker Desktop 4.x requires API version >= 1.44; override docker-java's default of 1.24
export DOCKER_API_VERSION=1.44
exec mvn "$@"
