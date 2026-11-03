"""Offline tests of listener registration and restoration without cmd or a device."""
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

TOOLS = Path(__file__).resolve().parents[1]
LISTENER = 'com.dangbei.tvlauncher/app.orchard.tv.guard.RevivalListener'
OTHER = 'example.reader/.Listener'
SIBLING = LISTENER + 'Other'
INNER = 'example.reader/example.Reader$Inner'


class NotificationListenersTest(unittest.TestCase):
    def run_helper(self, action, initial, read_status=0):
        script, name = ('install.sh', 'allow_revival_listener') if action == 'add' else ('restore.sh', 'remove_revival_listener')
        source = (TOOLS / script).read_text()
        start = source.index(name + '() {')
        helper = source[start:source.index('\n}\n', start) + 3]
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            state = root / 'state'
            state.write_text(initial)
            adb = root / 'adb'
            adb.write_text('''#!/usr/bin/env python3
import json, os, shlex, sys
from pathlib import Path
args = sys.argv[1:]
if args[0] == 'shell':
    args = ['shell'] + shlex.split(' '.join(args[1:]))
with open(os.environ['CALLS'], 'a') as log:
    log.write(json.dumps(args) + '\\n')
state = Path(os.environ['STATE'])
if args[:3] == ['shell', 'cmd', 'notification']:
    sys.exit(127)
if args == ['shell', 'settings', 'get', 'secure', 'enabled_notification_listeners']:
    print(state.read_text())
    sys.exit(int(os.environ['READ_STATUS']))
if args[:5] == ['shell', 'settings', 'put', 'secure', 'enabled_notification_listeners']:
    state.write_text(args[5])
elif args == ['shell', 'settings', 'delete', 'secure', 'enabled_notification_listeners']:
    state.write_text('null')
else:
    sys.exit(99)
''')
            adb.chmod(0o755)
            harness = root / 'harness.sh'
            harness.write_text('set -euo pipefail\nPKG=com.dangbei.tvlauncher\nNS=app.orchard.tv\n' + helper + '\n' + name + '\n' + name + '\n')
            env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ['PATH'],
                       CALLS=str(root / 'calls'), STATE=str(state), READ_STATUS=str(read_status))
            result = subprocess.run(['bash', str(harness)], env=env, capture_output=True, text=True, timeout=5)
            self.assertEqual(result.returncode, 0, result.stderr)
            calls = [json.loads(line) for line in (root / 'calls').read_text().splitlines()]
            mutations = [call for call in calls if call[:3] in (['shell', 'settings', 'put'], ['shell', 'settings', 'delete'])]
            self.assertFalse(any('accessibility' in item for call in calls for item in call))
            return state.read_text(), mutations

    def test_add_preserves_other_listeners_and_is_idempotent(self):
        for initial in ('null', '', OTHER, OTHER + ':' + SIBLING, OTHER + '\r', INNER):
            with self.subTest(initial=initial):
                state, mutations = self.run_helper('add', initial)
                prefix = initial.replace('\r', '')
                expected = LISTENER if prefix in ('null', '') else prefix + ':' + LISTENER
                self.assertEqual(state, expected)
                self.assertEqual(len(mutations), 1)

    def test_existing_exact_component_is_not_duplicated(self):
        for initial in (LISTENER, LISTENER + ':' + OTHER, OTHER + ':' + LISTENER):
            with self.subTest(initial=initial):
                state, mutations = self.run_helper('add', initial)
                self.assertEqual(state, initial)
                self.assertEqual(mutations, [])

    def test_restore_removes_only_exact_component(self):
        for initial in (LISTENER, OTHER + ':' + LISTENER, LISTENER + ':' + OTHER, OTHER + ':' + LISTENER + ':' + SIBLING, INNER + ':' + LISTENER):
            with self.subTest(initial=initial):
                state, mutations = self.run_helper('remove', initial)
                expected = ':'.join(part for part in initial.split(':') if part != LISTENER) or 'null'
                self.assertEqual(state, expected)
                self.assertEqual(len(mutations), 1)

    def test_restore_without_our_component_never_writes(self):
        for initial in ('null', '', OTHER, SIBLING):
            with self.subTest(initial=initial):
                state, mutations = self.run_helper('remove', initial)
                self.assertEqual(state, initial)
                self.assertEqual(mutations, [])

    def test_failed_or_malformed_read_never_overwrites_list(self):
        for action in ('add', 'remove'):
            for initial, status in ((OTHER, 1), ('settings: not found', 0), ('', 1), (OTHER + ':bad entry', 0)):
                with self.subTest(action=action, initial=initial, status=status):
                    state, mutations = self.run_helper(action, initial, status)
                    self.assertEqual(state, initial)
                    self.assertEqual(mutations, [])


if __name__ == '__main__':
    unittest.main()
