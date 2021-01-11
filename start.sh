#!/bin/bash
set -e

# define ADB_ARGS="-s <device ID>" to have this work w/ specific device
adb $ADB_ARGS uninstall life.nosk.motator
adb $ADB_ARGS install bin/motator.apk
adb $ADB_ARGS shell am start -n life.nosk.motator/life.nosk.motator.MainActivity
