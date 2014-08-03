#!/bin/bash

adb $1 shell 'rm -rf /sdcard/HueScripts'
adb $1 shell 'mkdir /sdcard/HueScripts'
adb $1 push -p scripts/. /sdcard/HueScripts/