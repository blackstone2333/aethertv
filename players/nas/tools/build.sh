#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -z "${JAVA_HOME:-}" ]; then
  for task_jdk in .runtime/jdk-21*/Contents/Home /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home; do
    if [ -x "$task_jdk/bin/java" ]; then JAVA_HOME="$PWD/$task_jdk"; [ "${task_jdk#/}" = "$task_jdk" ] || JAVA_HOME="$task_jdk"; export JAVA_HOME; break; fi
  done
fi
[ -n "${JAVA_HOME:-}" ] || { echo 'Set JAVA_HOME to a JDK21 installation.' >&2; exit 1; }
"$JAVA_HOME/bin/java" -version 2>&1 | head -n 1 | grep '"21\.' >/dev/null || { echo 'This independent app requires JDK21.' >&2; exit 1; }
exec ./gradlew --no-daemon testReleaseUnitTest assembleRelease "$@"
