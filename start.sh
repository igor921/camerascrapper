#!/usr/bin/env bash
fuser -n tcp -k 8082
killall ffmpeg
git pull
mvn clean package
cd target/
nohup java -jar scrapper.jar &