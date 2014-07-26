#!/bin/bash

tar -czvLf /tmp/scripts.tar.gz --exclude ^libs -C scripts .
adb push /tmp/scripts.tar.gz /sdcard/scripts.tar.gz
adb shell 'rm -rf /sdcard/HueScripts/*'
adb shell 'tar -zxvf /sdcard/scripts.tar.gz -C /sdcard/Huescripts'
