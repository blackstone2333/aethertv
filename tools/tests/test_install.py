"""Offline regression tests: every adb call is a stub; no TV or network is used."""
import os
import pathlib
import subprocess
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / 'install.sh'


class InstallFailureTest(unittest.TestCase):
    def run_install(self, failure, status, flavor='standard', model='maserati', model_status=0,
                    inspector='guard', installed='standard', real_apk=None, real_sdk=None):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            log = root / 'calls'
            adb = root / 'adb'
            adb.write_text('''#!/usr/bin/env bash
printf '%s\\n' "$*" >> "$CALL_LOG"
case "$*" in
  devices) printf 'List of devices attached\\nexample.invalid:5555 device\\n' ;;
  connect*) echo connected ;;
  get-state) echo device ;;
  'shell getprop ro.product.model') echo "$MODEL"; exit "$MODEL_STATUS" ;;
  'shell getprop ro.build.version.sdk') echo 30 ;;
  'shell dumpsys package com.dangbei.tvlauncher')
    if [ -f "$MOCK_STATE" ]; then echo versionCode=2; else echo versionCode=1; fi
    echo "versionName=0.3.1$INSTALLED_SUFFIX" ;;
  install*) echo "$INSTALL_OUTPUT"; exit "$INSTALL_STATUS" ;;
  push*) touch "$MOCK_STATE" ;;
  'shell settings get secure enabled_notification_listeners') echo null ;;
  'shell dumpsys notification') echo com.dangbei.tvlauncher/app.orchard.tv.guard.RevivalListener ;;
  'shell service check accessibility') echo 'Service accessibility: found' ;;
  'shell dumpsys accessibility') echo label=Aerial ;;
  'shell settings get secure screensaver_enabled') echo 0 ;;
  'shell settings get secure enabled_accessibility_services') echo com.dangbei.tvlauncher/app.orchard.tv.guard.HomeGuardService ;;
  'shell getprop persist.vt.use_home_pkg'|'shell pm path com.mitv.tvhome'|'shell pm path com.dangbeimarket') ;;
  'shell cmd package resolve-activity'*) echo com.dangbei.tvlauncher/app.orchard.tv.MainActivity ;;
  'shell am start -W'*) echo 'Status: ok' ;;
  'shell pm grant '*|'shell appops set '*|'shell cmd notification '*|'shell cmd package compile '*|'shell cmd role '*|'shell settings put '*|'shell content call '*|'shell rm -f /sdcard/Download/Aerial.apk') ;;
  *) echo "unexpected adb: $*" >&2; exit 99 ;;
esac
''')
            adb.chmod(0o755)
            if inspector != 'missing':
                tool = root / 'build-tools/fixture/aapt2'
                tool.parent.mkdir(parents=True)
                tool.write_text('''#!/usr/bin/env bash
printf 'aapt2 %s\\n' "$*" >> "$CALL_LOG"
[ "$INSPECTOR" != broken ] || exit 1
case "$2" in
  badging)
    package=com.dangbei.tvlauncher; suffix=-guard
    [ "$INSPECTOR" != package ] || package=app.orchard.tv
    [ "$INSPECTOR" != standard ] || suffix=
    echo "package: name='$package' versionCode='2' versionName='0.3.1$suffix' platformBuildVersionName='16'" ;;
  xmltree)
    [ "$INSPECTOR" != empty ] || exit 0
    echo 'E: manifest'
    [ "$INSPECTOR" = component ] || echo '  A: android:name="app.orchard.tv.MainActivity"'
    [ "$INSPECTOR" != home ] || echo '  A: android:name="android.intent.category.HOME"' ;;
esac
exit 0
''')
                tool.chmod(0o755)
            for name, command in [('sleep', 'exit 0'), ('nc', 'exit 1'),
                                  ('curl', 'echo "unexpected network" >&2; exit 99')]:
                stub = root / name
                stub.write_text('#!/usr/bin/env bash\n' + command + '\n')
                stub.chmod(0o755)
            env = dict(os.environ, PATH=str(root)+os.pathsep+os.environ['PATH'],
                       TV='example.invalid', APK_FILE=str(root/'test.apk'),
                       CALL_LOG=str(log), INSTALL_OUTPUT=failure,
                       INSTALL_STATUS=str(status), STAY_CONNECTED='1',
                       MOCK_STATE=str(root/'pushed'), BUILD='0', KEEP_ADB='0',
                       MODEL=model, MODEL_STATUS=str(model_status), INSPECTOR=inspector,
                       INSTALLED_SUFFIX='-guard' if installed == 'guard' else '',
                       ANDROID_HOME=str(root))
            if real_apk:
                env.update(APK_FILE=str(real_apk), ANDROID_HOME=str(real_sdk))
            result = subprocess.run(['bash', str(SCRIPT), flavor], env=env,
                                    capture_output=True, text=True, timeout=5)
            return result, log.read_text()

    def test_nonzero_certificate_mismatch_reaches_explanation(self):
        result, calls = self.run_install('Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]', 1)
        self.assertEqual(result.returncode, 1)
        self.assertIn('signed with another key', result.stderr)
        self.assertNotIn('pm grant', calls)
        self.assertNotIn('uninstall', calls)

    def test_nonzero_inconsistent_certificate_reaches_explanation(self):
        result, _ = self.run_install('Failure [INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES]', 1)
        self.assertIn('signed with another key', result.stderr)

    def test_zero_exit_failure_is_still_rejected(self):
        result, calls = self.run_install('Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]', 0)
        self.assertEqual(result.returncode, 1)
        self.assertIn('signed with another key', result.stderr)
        self.assertNotIn('pm grant', calls)

    def test_nonzero_generic_failure_prints_error_and_guard_hint(self):
        result, calls = self.run_install('Failure [INSTALL_FAILED_INVALID_APK]', 7)
        self.assertEqual(result.returncode, 1)
        self.assertIn('INSTALL_FAILED_INVALID_APK', result.stderr)
        self.assertIn('Try:', result.stderr)
        self.assertNotIn('pm grant', calls)

    def test_success_reaches_home_confirmation(self):
        result, calls = self.run_install('Success', 0)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('Aerial is the home screen', result.stdout)
        self.assertIn('shell pm grant', calls)

    def test_only_a_success_line_with_zero_exit_accepts_install(self):
        for output, status in (('', 0), ('Not Success', 0), ('Success', 1), ('Failure [Success]', 0)):
            with self.subTest(output=output, status=status):
                result, calls = self.run_install(output, status)
                self.assertEqual(result.returncode, 1)
                self.assertNotIn('pm grant', calls)
                self.assertNotIn('push ', calls)

    def test_carriage_returns_and_adb_status_lines_preserve_success(self):
        result, calls = self.run_install('Performing Streamed Install\r\nSuccess\r', 0)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('shell pm grant', calls)

    def test_nonzero_verifier_failure_reaches_existing_tcl_fallback(self):
        result, calls = self.run_install('Failure [INSTALL_FAILED_VERIFICATION_FAILURE]', 1, 'guard')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn('now in Download', result.stdout)
        self.assertIn('Home Guard is running', result.stdout)
        self.assertIn('push ', calls)

    def test_tcl_51_textual_refusal_uses_the_guard_fallback_for_both_shell_statuses(self):
        for status in (0, 1):
            with self.subTest(status=status):
                result, calls = self.run_install(
                    'install apk has be disabled from pm by system default!', status, 'guard')
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertIn('now in Download', result.stdout)
                self.assertIn('push ', calls)

    def assert_no_mutations(self, calls):
        for command in ('install -r', 'push ', 'pm grant', 'appops set', 'settings put',
                        'settings delete', 'cmd role', 'set-home-activity', 'am start',
                        'force-stop', 'uninstall', 'pm clear'):
            self.assertNotIn(command, calls)

    def test_mt5873_standard_stops_before_build_download_inspection_or_install(self):
        for installed in ('standard', 'guard'):
            with self.subTest(installed=installed):
                result, calls = self.run_install('Success', 0, model='mt5873\r', installed=installed)
                self.assertEqual(result.returncode, 1)
                self.assertIn('can hang at boot', result.stderr)
                self.assertIn("rerun with 'guard'", result.stderr)
                self.assertNotIn('aapt2', calls)
                self.assert_no_mutations(calls)

    def test_failed_or_unrecognized_model_stops_without_mutation(self):
        for model, status in (('mt5873', 1), ('', 0), ('null', 0),
                              ('getprop: not found', 0), ('error: closed', 0), ('first\nsecond', 0)):
            with self.subTest(model=model, status=status):
                result, calls = self.run_install('Success', 0, model=model, model_status=status)
                self.assertEqual(result.returncode, 1)
                self.assertIn('model', result.stderr)
                self.assert_no_mutations(calls)

    def test_mt5873_guard_requires_consistent_actual_apk_metadata(self):
        for inspector in ('missing', 'broken', 'empty', 'standard', 'package', 'component', 'home'):
            with self.subTest(inspector=inspector):
                result, calls = self.run_install('Success', 0, 'guard', model='mt5873', inspector=inspector)
                self.assertEqual(result.returncode, 1, result.stderr)
                self.assertIn('nothing was installed', result.stderr)
                self.assert_no_mutations(calls)

    def test_mt5873_verified_guard_can_update_either_installed_flavor(self):
        for installed in ('standard', 'guard'):
            with self.subTest(installed=installed):
                result, calls = self.run_install('Success', 0, 'guard', model='mt5873', installed=installed)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertIn('aapt2 dump xmltree', calls)
                self.assertIn('install -r', calls)
                self.assertNotIn('cmd role', calls)
                self.assertNotIn('set-home-activity', calls)

    def test_mt5873_guard_signature_failure_never_uninstalls_or_continues_setup(self):
        result, calls = self.run_install('Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE]', 1,
                                         'guard', model='mt5873')
        self.assertEqual(result.returncode, 1)
        self.assertIn('signed with another key', result.stderr)
        self.assertNotIn('pm grant', calls)
        self.assertNotIn('uninstall', calls)

    def test_other_mstar_models_keep_the_requested_standard_without_the_inspector(self):
        for model in ('maserati', 'mt5873-other'):
            with self.subTest(model=model):
                result, calls = self.run_install('Success', 0, model=model, inspector='missing')
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertIn('install -r', calls)
                self.assertNotIn('aapt2', calls)


if __name__ == '__main__':
    unittest.main()
