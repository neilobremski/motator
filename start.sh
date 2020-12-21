#!/bin/bash
set -e
adb install bin/motator.apk
adb shell am start -n life.nosk.motator/life.nosk.motator.MainActivity
