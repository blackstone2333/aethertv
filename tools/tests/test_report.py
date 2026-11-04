"""Exercise report.sh's complete masking function with synthetic, offline text only."""
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

SCRIPT = Path(os.environ.get('REPORT_SCRIPT', Path(__file__).resolve().parents[1] / 'report.sh')).resolve()


class ReportMaskTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # Do not source the script: its top-level code would connect to a TV.
        match = re.search(r'^mask\(\) \{\n.*?^\}', SCRIPT.read_text(), re.M | re.S)
        if match is None:
            raise AssertionError('report.sh mask() function was not found')
        cls.function = match.group()

    def mask(self, text):
        env = dict(os.environ, LC_ALL='C')
        for name in ('SHELLOPTS', 'BASHOPTS', 'BASH_ENV'):
            env.pop(name, None)
        result = subprocess.run(['bash', '-c', 'set -euo pipefail\n' + self.function + '\nmask'],
                                input=text, capture_output=True, text=True, env=env, timeout=5)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stderr, '')
        return result.stdout

    def test_adjacent_addresses_share_one_separator(self):
        for separator in (' ', ',', ':', '\t', ';'):
            with self.subTest(separator=separator):
                source = separator.join(('192.0.2.1', '198.51.100.2', '203.0.113.3')) + '\n'
                expected = separator.join(('<ip>', '<ip>', '<ip>')) + '\n'
                self.assertEqual(self.mask(source), expected)

    def test_addresses_on_separate_lines(self):
        self.assertEqual(self.mask('192.0.2.1\n198.51.100.2\n'), '<ip>\n<ip>\n')

    def test_long_adjacent_list_terminates_and_masks_every_address(self):
        source = ','.join('192.0.2.' + str(n) for n in range(1, 101)) + '\n'
        self.assertEqual(self.mask(source), ','.join(['<ip>'] * 100) + '\n')

    def test_address_boundaries_and_surrounding_text_are_kept(self):
        for source, expected in (
                ('192.0.2.1\n', '<ip>\n'),
                ('[192.0.2.1]\n', '[<ip>]\n'),
                ('(192.0.2.1),198.51.100.2\n', '(<ip>),<ip>\n'),
                ('from=192.0.2.1 to=198.51.100.2:5555\n', 'from=<ip> to=<ip>:5555\n'),
                ('version v1.2.3.4 peer 192.0.2.1\n', 'version v1.2.3.4 peer <ip>\n')):
            with self.subTest(source=source):
                self.assertEqual(self.mask(source), expected)

    def test_dotted_versions_identifiers_and_longer_numbers_are_unchanged(self):
        source = ('v1.2.3.4 1.2.3.4beta prefix192.0.2.1 192.0.2.1suffix '
                  '192.0.2.1.5 .192.0.2.1 192.0.2.1. 1192.0.2.1 192.0.2.1111 1.80\n')
        self.assertEqual(self.mask(source), source)

    def test_phone_tokens_and_mac_addresses_are_still_masked(self):
        source = ('http://192.0.2.1:8790/AbC123/api '
                  'aa:bb:cc:dd:ee:ff AA:BB:CC:DD:EE:FF :8790/Z9\n')
        self.assertEqual(self.mask(source), 'http://<ip>:8790/<token>/api <mac> <mac> :8790/<token>\n')

    def test_mixed_masks_do_not_leave_adjacent_addresses(self):
        source = ('192.0.2.1,198.51.100.2 http://203.0.113.3:8790/AbC123 '
                  '00:11:22:33:44:55 192.0.2.4:198.51.100.5\n')
        expected = '<ip>,<ip> http://<ip>:8790/<token> <mac> <ip>:<ip>\n'
        self.assertEqual(self.mask(source), expected)

    def test_masking_is_idempotent(self):
        source = '192.0.2.1 198.51.100.2 :8790/AbC123 aa:bb:cc:dd:ee:ff\n'
        masked = self.mask(source)
        self.assertEqual(self.mask(masked), masked)

    def test_empty_input_and_existing_markers(self):
        self.assertEqual(self.mask(''), '')
        self.assertEqual(self.mask('<ip> <token> <mac>\n'), '<ip> <token> <mac>\n')


class ReportCollectionTest(unittest.TestCase):
    def collect(self, mode):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            adb = root / 'adb'
            adb.write_text('''#!/usr/bin/env bash
printf '%s\\n' "$*" >> "$CALL_LOG"
case "$*" in
  connect*) echo connected ;;
  get-state) echo device ;;
  'shell command -v cmd')
    [ "$MODE" = legacy ] && exit 127
    echo /system/bin/cmd ;;
  'shell getprop ro.product.model') echo OfflineTV ;;
  'shell getprop ro.build.version.sdk') echo 23 ;;
  'shell getprop '*) ;;
  'shell wm size') echo 'Physical size: 1920x1080' ;;
  'shell wm density') echo 'Physical density: 320' ;;
  'shell dumpsys package com.dangbei.tvlauncher') echo 'versionName=0.3.1' ;;
  'shell appops get '*) ;;
  'shell cmd package resolve-activity '*)
    if [ "$MODE" = broken_cmd ]; then echo 'cmd: unsupported package command'; exit 1; fi
    echo example.stock/.Home ;;
  'shell cmd package query-activities '*) echo example.stock/.Home ;;
  'shell cmd role get-role-holders '*) echo example.stock ;;
  'shell dumpsys activity activities') ;;
  'shell settings get '*) echo null ;;
  'shell dumpsys accessibility') ;;
  'shell dumpsys notification') echo com.dangbei.tvlauncher/app.orchard.tv.guard.RevivalListener ;;
  'shell dumpsys tv_input') ;;
  'shell logcat '*)
    echo ' I/AerialUpdates(123): peers 192.0.2.1,198.51.100.2' ;;
  'shell dumpsys activity com.dangbei.tvlauncher/app.orchard.tv.MainActivity report')
    [ "$MODE" = no_app ] && exit 1
    echo '### Aerial diagnostics'
    echo 'Home: offline read-only evidence'
    echo 'peers 192.0.2.1,198.51.100.2' ;;
  *) echo "unexpected adb: $*" >&2; exit 99 ;;
esac
''')
            adb.chmod(0o755)
            clipboard = root / 'pbcopy'
            clipboard.write_text('#!/usr/bin/env bash\ncat >/dev/null\n')
            clipboard.chmod(0o755)
            env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ['PATH'],
                       CALL_LOG=str(root / 'calls'), MODE=mode, TV='example.invalid')
            result = subprocess.run(['bash', str(SCRIPT)], cwd=root, env=env,
                                    capture_output=True, text=True, timeout=5)
            report = root / 'aethertv-report-OfflineTV.md'
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertTrue(report.exists())
            text = report.read_text()
            self.assertIn(text, result.stdout)
            self.assertIn('https://github.com/blackstone2333/aethertv/issues/new', result.stderr)
            self.assertNotIn('unexpected adb:', result.stderr)
            calls = (root / 'calls').read_text().splitlines()
            allowed = ('connect ', 'get-state', 'shell getprop ', 'shell command -v cmd',
                       'shell wm ', 'shell dumpsys ', 'shell appops get ', 'shell settings get ',
                       'shell cmd package resolve-activity ', 'shell cmd package query-activities ',
                       'shell cmd role get-role-holders ', 'shell logcat ')
            self.assertTrue(all(call.startswith(allowed) for call in calls), calls)
            self.assertIn('notification listener: 1 mentions', text)
            self.assertIn('peers <ip>,<ip>', text)
            self.assertNotIn('192.0.2.1', text)
            self.assertNotIn('198.51.100.2', text)
            return text, calls

    def test_legacy_report_completes_without_cmd_or_optional_services(self):
        text, calls = self.collect('legacy')
        self.assertIn('resolves to: (no cmd on this Android', text)
        self.assertIn('Home: offline read-only evidence', text)
        self.assertFalse(any(call.startswith('shell cmd ') for call in calls))

    def test_modern_report_accepts_only_a_resolved_component(self):
        text, _ = self.collect('modern')
        self.assertIn('resolves to: example.stock/.Home', text)
        self.assertIn('role holder: example.stock', text)
        self.assertIn('Home: offline read-only evidence', text)

    def test_shell_error_is_never_reported_as_default_home(self):
        text, _ = self.collect('broken_cmd')
        self.assertIn('resolves to: (unavailable;', text)
        self.assertNotIn('cmd: unsupported', text)

    def test_missing_app_report_does_not_start_an_application(self):
        text, calls = self.collect('no_app')
        self.assertIn('App diagnostics unavailable', text)
        self.assertFalse(any('am start' in call for call in calls))


if __name__ == '__main__':
    unittest.main()
