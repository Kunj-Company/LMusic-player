#!/bin/bash
# Installs each built APK, launches it, and records logcat + crash buffer.
set -x
mkdir -p launch-logs
run() {
  local apk="$1" pkg="$2" name="$3"
  adb uninstall "$pkg" >/dev/null 2>&1
  adb install -r -g "$apk" || { echo "INSTALL FAILED: $apk"; return; }
  adb logcat -c; adb logcat -b crash -c
  adb shell am start -W -n "$pkg/com.twilighty.lmusic.MainActivity"
  sleep 25
  adb shell pidof "$pkg" && echo "$name: PROCESS ALIVE" || echo "$name: PROCESS DEAD"
  adb shell dumpsys activity activities | grep -i "mResumedActivity\|topResumedActivity" | head -3
  adb exec-out screencap -p > "launch-logs/$name.png"
  adb logcat -d -b crash > "launch-logs/$name-crash.txt"
  adb logcat -d > "launch-logs/$name-logcat.txt"
  echo "===== $name crash buffer ====="
  cat "launch-logs/$name-crash.txt"
  echo "===== $name AndroidRuntime ====="
  grep -A40 "FATAL EXCEPTION\|AndroidRuntime" "launch-logs/$name-logcat.txt" | head -120
}
REL=$(ls app/build/outputs/apk/release/*.apk | head -1)
DBG=$(ls app/build/outputs/apk/debug/*.apk | head -1)
run "$REL" com.twilighty.lmusic release
run "$DBG" com.twilighty.lmusic.debug debug
exit 0
