#!/usr/bin/env bash
# Usage: check-minos.sh <version> <Mach-O binary or static archive>
# Fails unless every LC_BUILD_VERSION load command has "minos <version>", except for archive members
# listed in minos-exempt.txt next to this script.
set -euo pipefail
expected="$1"
binary="$2"
exempt=$(grep -vE '^[[:space:]]*(#|$)' "$(dirname "$0")/minos-exempt.txt" || true)
# One line per load command: "<archive member or (binary)> <minos>".
report=$(otool -l "$binary" | awk '
  /\(.*\):$/ { member = $0; sub(/^.*\(/, "", member); sub(/\):$/, "", member) }
  /LC_BUILD_VERSION/ { inside = 1 }
  inside && $1 == "minos" { print (member == "" ? "(binary)" : member), $2; inside = 0 }')
if [ -z "$report" ]; then
  echo "No LC_BUILD_VERSION load commands in $binary" >&2
  exit 1
fi
echo "minos values in $binary:"
echo "$report" | awk '{ print $2 }' | sort | uniq -c
offenders=$(echo "$report" | awk -v want="$expected" '$2 != want' | sort -u | while read -r name version; do
  grep -qxF "$name" <<<"$exempt" || echo "  $name: minos $version"
done)
if [ -n "$offenders" ]; then
  echo "Expected minos $expected; these objects differ and are not in minos-exempt.txt:" >&2
  echo "$offenders" >&2
  exit 1
fi
exempted=$(echo "$report" | awk -v want="$expected" '$2 != want { print $1 }' | sort -u | tr '\n' ' ')
if [ -n "$exempted" ]; then echo "Exempt (minos-exempt.txt): $exempted"; fi
