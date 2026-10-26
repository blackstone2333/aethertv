#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$repo_root"

fail() { echo "project rules: $*" >&2; exit 1; }

grep -Eq 'applicationId = "com\.dangbei\.tvlauncher"' app/build.gradle.kts \
  || fail 'applicationId must remain com.dangbei.tvlauncher'
grep -Eq 'namespace = "app\.orchard\.tv"' app/build.gradle.kts \
  || fail 'namespace must remain app.orchard.tv'
grep -q 'com.dangbei.tvlauncher.CONFIG' app/src/main/AndroidManifest.xml \
  || fail 'manifest CONFIG action must use the whitelist package'
grep -q 'category android:name="android.intent.category.HOME"' app/src/standard/AndroidManifest.xml \
  || fail 'standard flavor must keep the HOME filter'

for required in \
  docs/PROJECT.md docs/AGENT_WORKFLOW.md docs/work docs/handoffs \
  docs/work/AERIAL-20261026-roadmap-phase2.md docs/handoffs/TEMPLATE.md; do
  [ -e "$required" ] || fail "required project record is missing: $required"
done

java_bin="${JAVA_HOME:-}/bin/java"
if [ ! -x "$java_bin" ]; then java_bin="$(command -v java || true)"; fi
[ -n "$java_bin" ] && [ -x "$java_bin" ] || fail 'Java 17 is required; set JAVA_HOME=/opt/homebrew/opt/openjdk@17'
java_version="$($java_bin -version 2>&1 | sed -n '1s/.*version "\([0-9][0-9]*\)\..*/\1/p')"
[ "$java_version" = 17 ] || fail "Java 17 is required; found ${java_version:-unknown}"

if git diff --name-only -- local/ | grep -q .; then
  fail 'local/ changes must not be part of a project change'
fi

printf '%s\n' 'project rules: OK (Dangbei package, namespace, HOME filter, records, Java 17)'
