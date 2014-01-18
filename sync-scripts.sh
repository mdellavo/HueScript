#!/bin/bash

tar -czvf /tmp/scripts.tar.gz -C scripts .
adb push /tmp/scripts.tar.gz /sdcard/scripts.tar.gz
adb shell 'tar -zxvf /sdcard/scripts.tar.gz -C /sdcard/HueScripts'
