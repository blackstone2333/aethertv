"""Regenerate a tiny GeoNames fixture without downloading or changing project assets."""
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'make-cities.py'


class CurrentCityNamesTest(unittest.TestCase):
    def generate(self, *cities):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / 'tools').mkdir()
            (root / 'app/src/main/assets').mkdir(parents=True)
            script = root / 'tools/make-cities.py'
            shutil.copy2(SCRIPT, script)
            fixture = root / 'CN.txt'
            records = [('湖北省,湖北', 'ADM1'), *cities]
            lines = []
            for names, code in records:
                row = [''] * 19
                row[2:8] = ['Xiangyang', names, '32.0422', '112.1448', 'P', code]
                row[10], row[14] = '12', '1000'
                lines.append('\t'.join(row))
            fixture.write_text('\n'.join(lines) + '\n', encoding='utf-8')
            result = subprocess.run([sys.executable, '-I', str(script), str(fixture)],
                                    capture_output=True, text=True, timeout=5)
            self.assertEqual(result.returncode, 0, result.stderr)
            return [line.split('\t') for line in
                    (root / 'app/src/main/assets/cities-cn.tsv').read_text(encoding='utf-8').splitlines()
                    if not line.startswith('#')]

    def test_old_name_majority_keeps_current_name_and_all_aliases(self):
        rows = self.generate(('襄樊,襄樊市,襄阳,樊城', 'PPLA2'))
        self.assertEqual(len(rows), 1)
        self.assertEqual(rows[0][0], '襄阳')
        self.assertEqual(set(rows[0][1].split(',')), {'襄樊', '樊城'})
        self.assertEqual(rows[0][2:], ['湖北', '32.0422', '112.1448', '1', 'Xiangyang'])

    def test_missing_current_alias_is_added_without_losing_the_old_name(self):
        row = self.generate(('襄樊市,襄樊', 'PPLA2'))[0]
        self.assertEqual(row[0], '襄阳')
        self.assertEqual(row[1], '襄樊')

    def test_canonicalization_happens_before_county_dedup_and_leaves_other_names(self):
        rows = self.generate(('襄樊市,襄樊', 'PPLA2'), ('襄阳', 'ADM3'), ('武汉市,武汉', 'PPLA'))
        self.assertEqual([row[0] for row in rows], ['武汉', '襄阳'])


if __name__ == '__main__':
    unittest.main()
