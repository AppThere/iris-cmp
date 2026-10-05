#!/usr/bin/env bash
# Usage: check-minos.sh <version> <Mach-O binary or static archive>
# Fails unless every LC_BUILD_VERSION load command in the binary has "minos <version>".
set -euo pipefail
expected="$1"
binary="$2"
found=$(otool -l "$binary" | awk '/LC_BUILD_VERSION/ { inside = 1 } inside && $1 == "minos" { print $2; inside = 0 }' | sort | uniq -c)
if [ -z "$found" ]; then
  echo "No LC_BUILD_VERSION load commands in $binary" >&2
  exit 1
fi
echo "minos values in $binary:"
echo "$found"
if echo "$found" | awk '{ print $2 }' | grep -vqx "$expected"; then
  echo "Expected every object to have minos $expected" >&2
  exit 1
fi
