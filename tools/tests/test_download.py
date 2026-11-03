"""Offline APK download checks: real unzip, stubbed curl and adb, no TV/network."""
import os
import pathlib
import shutil
import subprocess
import tempfile
import unittest
import zipfile

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / 'install.sh'


class DownloadTest(unittest.TestCase):
    def run_download(self, kind='valid', flavor='standard', no_unzip=False, local_apk=False):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            bindir = root / 'bin'
            bindir.mkdir()
            # A stored member lets us corrupt its data without changing the CRC.
            with zipfile.ZipFile(root / 'valid', 'w') as archive:
                archive.writestr('AndroidManifest.xml', b'fixture manifest payload')
                archive.writestr('classes.dex', b'fixture dex payload')
            valid = (root / 'valid').read_bytes()
            (root / 'truncated').write_bytes(valid[:len(valid)//2])
            (root / 'crc').write_bytes(valid.replace(b'fixture dex payload', b'corrupt dex payload'))
            (root / 'html').write_bytes(b'<html>gateway error</html>')
            (root / 'empty').write_bytes(b'')
            old = root / ('aethertv-' + flavor + '.apk')
            old.write_bytes(b'previous file: do not remove on failure')
            scripts = {
                'curl': '''#!/usr/bin/env bash
printf '%s\\n' "$*" >> "$CURL_LOG"
out=
while [ "$#" -gt 0 ]; do
  case "$1" in
    -o) out=$2; shift 2 ;;
    http*) url=$1; shift ;;
    *) shift ;;
  esac
done
case "$url" in
  https://github.com/blackstone2333/aethertv/releases/latest/download/aethertv-*) kind=$KIND ;;
  *) exit 99 ;;
esac
if [ "$kind" = network ]; then
  printf partial > "$out"
  exit 18
fi
cp "$FIXTURES/$kind" "$out"
''',
                'adb': '''#!/usr/bin/env bash
printf '%s\\n' "$*" >> "$CALL_LOG"
case "$*" in
  connect*) echo connected ;;
  get-state) echo device ;;
  'shell getprop ro.build.version.sdk') echo "$SDK" ;;
  'shell dumpsys package com.dangbei.tvlauncher') echo versionCode=1 ;;
  install*)
    cp "$3" "$INSTALLED"
    # Stop immediately after recording the bytes supplied to installation.
    echo 'Failure [INSTALL_FAILED_INVALID_APK]'
    exit 1 ;;
  *) echo "unexpected adb: $*" >&2; exit 99 ;;
esac
''',
            }
            for name, body in scripts.items():
                path = bindir / name
                path.write_text(body)
                path.chmod(0o755)
            # A minimal PATH proves missing unzip fails closed, not silently unvalidated.
            if no_unzip:
                for name in ('bash', 'dirname', 'tr', 'sed', 'head', 'cp'):
                    (bindir / name).symlink_to(shutil.which(name))
            env = dict(os.environ, PATH=str(bindir) if no_unzip else str(bindir)+os.pathsep+os.environ['PATH'],
                       TV='example.invalid', TMPDIR=str(root), BUILD='0', APK_FILE='',
                       STAY_CONNECTED='1', ANDROID_HOME=str(root), SDK='30',
                       KIND=kind, FIXTURES=str(root),
                       CALL_LOG=str(root/'adb.log'), CURL_LOG=str(root/'curl.log'),
                       INSTALLED=str(root/'installed'))
            if local_apk:
                env['APK_FILE'] = str(root/'valid')
            result = subprocess.run(['bash', str(SCRIPT), flavor], env=env,
                                    capture_output=True, text=True, timeout=5)
            return {
                'result': result,
                'calls': (root/'adb.log').read_text(),
                'downloads': (root/'curl.log').read_text().splitlines() if (root/'curl.log').exists() else [],
                'installed': (root/'installed').read_bytes() if (root/'installed').exists() else None,
                'valid': valid, 'output': old.read_bytes(),
                'leftovers': [p.name for p in root.glob('aethertv-*') if p != old],
            }

    def test_missing_unzip_fails_closed_before_download(self):
        got = self.run_download(no_unzip=True)
        self.assertIn('unzip is required', got['result'].stderr)
        self.assertEqual(got['downloads'], [])
        self.assertIsNone(got['installed'])
        self.assertEqual(got['output'], b'previous file: do not remove on failure')
        self.assertEqual(got['leftovers'], [])

    def test_local_apk_does_not_require_unzip_or_download(self):
        got = self.run_download(no_unzip=True, local_apk=True)
        self.assertEqual(got['downloads'], [])
        self.assertEqual(got['installed'], got['valid'])
        self.assertNotIn('unzip is required', got['result'].stderr)

    def test_valid_repository_download_all_flavors(self):
        for flavor in ('standard', 'guard'):
            with self.subTest(flavor=flavor):
                got = self.run_download(flavor=flavor)
                self.assertEqual(len(got['downloads']), 1)
                self.assertEqual(got['installed'], got['valid'])
                self.assertEqual(got['leftovers'], [])

    def test_bad_download_never_installs_and_preserves_old_file(self):
        for flavor in ('standard', 'guard'):
            for kind in ('truncated', 'crc', 'html', 'empty', 'network'):
                with self.subTest(flavor=flavor, kind=kind):
                    got = self.run_download(kind, flavor=flavor)
                    self.assertNotEqual(got['result'].returncode, 0)
                    self.assertIn('Could not download', got['result'].stderr)
                    self.assertEqual(len(got['downloads']), 1)
                    self.assertIsNone(got['installed'])
                    self.assertNotIn('install ', got['calls'])
                    self.assertNotIn('push ', got['calls'])
                    self.assertEqual(got['output'], b'previous file: do not remove on failure')
                    self.assertEqual(got['leftovers'], [])


if __name__ == '__main__':
    unittest.main()
