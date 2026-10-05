#!/usr/bin/env bash
# Usage: check-minos.sh <version> <Mach-O binary or static archive>
# Fails if any LC_BUILD_VERSION load command needs a newer OS than <version> (minos above it), except for
# archive members listed in minos-exempt.txt next to this script. Objects built for older versions are fine:
# prebuilt dependencies such as Skiko target Compose's own minimum.
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
# Version comparison on dotted numbers: is $2 newer than want?
newer='function newer(v, w,   a, b, i) { split(v, a, "."); split(w, b, "."); for (i = 1; i <= 3; i++) { if ((a[i] + 0) != (b[i] + 0)) return (a[i] + 0) > (b[i] + 0) } return 0 }'
offenders=$(echo "$report" | awk -v want="$expected" "$newer"' newer($2, want)' | sort -u | while read -r name version; do
  grep -qxF "$name" <<<"$exempt" || echo "  $name: minos $version"
done)
if [ -n "$offenders" ]; then
  echo "These objects need an OS newer than $expected and are not in minos-exempt.txt:" >&2
  echo "$offenders" >&2
  exit 1
fi
exempted=$(echo "$report" | awk -v want="$expected" "$newer"' newer($2, want) { print $1 }' | sort -u | tr '\n' ' ')
if [ -n "$exempted" ]; then echo "Exempt (minos-exempt.txt): $exempted"; fi
