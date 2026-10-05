#!/usr/bin/env bash
# Usage: ios-smoke.sh <path to .app> <bundle id> <screenshot.png>
# Boots an available iPhone simulator, installs and launches the app, and fails unless it is
# still running after 10 seconds. Saves a screenshot for the run's artifacts.
set -euo pipefail
app="$1"
bundle="$2"
shot="$3"
udid=$(xcrun simctl list devices available -j | python3 -c '
import json, sys
devices = json.load(sys.stdin)["devices"]
for runtime, entries in sorted(devices.items(), reverse=True):
    if "iOS" in runtime:
        for d in entries:
            if d["name"].startswith("iPhone"):
                print(d["udid"]); sys.exit(0)
sys.exit("no available iPhone simulator")')
echo "Simulator: $udid"
xcrun simctl bootstatus "$udid" -b
xcrun simctl install "$udid" "$app"
xcrun simctl launch "$udid" "$bundle"
sleep 10
if ! xcrun simctl spawn "$udid" launchctl list | grep -q "UIKitApplication:$bundle"; then
  echo "$bundle is not running 10 s after launch" >&2
  xcrun simctl spawn "$udid" log show --last 1m --predicate "process == \"Iris\"" --style compact | tail -50 >&2 || true
  exit 1
fi
xcrun simctl io "$udid" screenshot "$shot"
echo "$bundle is running"
