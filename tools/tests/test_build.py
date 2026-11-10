"""Offline BUILD=1 regression tests; Gradle, adb and curl are all stubs."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(os.environ.get('INSTALL_SCRIPT', Path(__file__).resolve().parents[1] / 'install.sh')).resolve()


class BuildFailureTest(unittest.TestCase):
    def run_build(self, flavor, status):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root/'tools').mkdir()
            script = root/'tools/install.sh'
            script.write_bytes(SCRIPT.read_bytes())
            apk = 'app/build/outputs/apk/{0}/release/app-{0}-release.apk'.format(flavor)
            artifact = root/apk
            artifact.parent.mkdir(parents=True)
            artifact.write_bytes(b'old artifact')
            scripts = {
                'gradlew': '''#!/usr/bin/env bash
printf 'gradle %s\\n' "$*" >> "$LOG"
[ "$GRADLE_STATUS" = 0 ] || { echo 'Build failed (fixture)' >&2; exit "$GRADLE_STATUS"; }
echo 'Build output must not become part of the APK path'
printf 'new artifact' > "$ARTIFACT"
''',
                'adb': '''#!/usr/bin/env bash
printf 'adb %s\\n' "$*" >> "$LOG"
case "$*" in
  connect*) echo connected ;;
  get-state) echo device ;;
  'shell getprop ro.product.model') echo maserati ;;
  'shell getprop ro.build.version.sdk') echo "$SDK" ;;
  'shell dumpsys package com.dangbei.tvlauncher') echo versionCode=1 ;;
  install*)
    cp "$3" "$INSTALLED"
    # Deliberately stop after observing bytes, without pretending a TV was installed.
    echo 'Failure [INSTALL_FAILED_INVALID_APK]'
    exit 1 ;;
  *) echo "unexpected adb: $*" >&2; exit 99 ;;
esac
''',
                'curl': '#!/usr/bin/env bash\necho "unexpected network" >&2; exit 99\n',
            }
            for name, body in scripts.items():
                file = root/name
                file.write_text(body)
                file.chmod(0o755)
            env = dict(os.environ, PATH=str(root)+os.pathsep+os.environ['PATH'],
                       BUILD='1', APK_FILE='', TV='example.invalid', STAY_CONNECTED='1',
                       JAVA_HOME=str(root), ANDROID_HOME=str(root), SDK='30',
                       LOG=str(root/'calls'), ARTIFACT=str(artifact), INSTALLED=str(root/'installed'),
                       GRADLE_STATUS=str(status))
            # Regression must exercise normal bash, not caller-injected POSIX/errexit inheritance.
            env.pop('SHELLOPTS', None)
            env.pop('BASHOPTS', None)
            env.pop('BASH_ENV', None)
            env.pop('POSIXLY_CORRECT', None)
            result = subprocess.run(['bash', str(script), flavor], env=env,
                                    capture_output=True, text=True, timeout=5)
            return result, (root/'calls').read_text(), artifact.read_bytes(), (
                (root/'installed').read_bytes() if (root/'installed').exists() else None)

    def test_failed_build_never_installs_existing_apk(self):
        for flavor in ('standard', 'guard'):
            with self.subTest(flavor=flavor):
                result, calls, artifact, installed = self.run_build(flavor, 42)
                self.assertEqual(result.returncode, 42, result.stderr)
                self.assertIn('Build failed (fixture)', result.stderr)
                self.assertNotIn('adb install ', calls)
                self.assertNotIn('adb push ', calls)
                self.assertNotIn('adb shell pm grant ', calls)
                self.assertIsNone(installed)
                self.assertEqual(artifact, b'old artifact')

    def test_successful_build_uses_new_artifact(self):
        for flavor in ('standard', 'guard'):
            with self.subTest(flavor=flavor):
                result, calls, artifact, installed = self.run_build(flavor, 0)
                task = 'assemble'+flavor.title()+'Release'
                self.assertIn('gradle -q --console=plain '+task, calls)
                self.assertIn('adb install -r ', calls)
                self.assertEqual(installed, b'new artifact')
                self.assertEqual(artifact, b'new artifact')
                self.assertIn('INSTALL_FAILED_INVALID_APK', result.stdout+result.stderr)


if __name__ == '__main__':
    unittest.main()
