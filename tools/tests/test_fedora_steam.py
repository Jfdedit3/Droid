import os
from pathlib import Path
import subprocess
import tempfile
import unittest

BIN = Path(__file__).resolve().parents[1] / 'linuxfs/overlay/usr/local/bin'


class FedoraSteamTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.home = self.root / 'home'
        self.home.mkdir()
        self.logs = self.root / 'logs'
        self.libs = self.root / 'lib64'
        (self.libs / 'pkcs11').mkdir(parents=True)
        (self.libs / 'pkcs11/p11-kit-trust.so').write_bytes(b'trust-provider')
        self.client = self.root / 'client'
        self.client.write_text('#!/bin/sh\nenv >"$TEST_ENV"\nexit "${TEST_EXIT:-0}"\n')
        self.client.chmod(0o755)
        self.script = self.root / 'steam'
        text = (BIN / 'droiddeck-fedora-steam').read_text()
        text = text.replace('/usr/lib64', str(self.libs)).replace('/usr/local/bin/droiddeck-steam-launch', str(self.client))
        text = text.replace('export LD_PRELOAD=', '# Test executable uses host libraries.\n# LD_PRELOAD=')
        self.script.write_text(text)
        self.env = dict(os.environ, HOME=str(self.home), DD_DEBUG_DIR=str(self.logs),
                        TEST_ENV=str(self.root / 'env'), GALLIUM_DRIVER='zink',
                        MESA_LOADER_DRIVER_OVERRIDE='zink', LIBGL_KOPPER_DRI2='true')

    def run_steam(self):
        return subprocess.run(['bash', str(self.script)], env=self.env, capture_output=True, text=True, timeout=10)

    def test_software_ui_and_valid_system_trust_module(self):
        self.assertEqual(self.run_steam().returncode, 0)
        self.assertEqual((self.libs / 'libnssckbi.so').read_bytes(), b'trust-provider')
        env = dict(line.split('=', 1) for line in (self.root / 'env').read_text().splitlines())
        self.assertEqual(env['GALLIUM_DRIVER'], 'llvmpipe')
        self.assertEqual(env['LIBGL_ALWAYS_SOFTWARE'], '1')
        self.assertNotIn('MESA_LOADER_DRIVER_OVERRIDE', env)
        self.assertNotIn('LIBGL_KOPPER_DRI2', env)
        self.assertFalse((self.logs / 'steam-failed').exists())
        self.assertEqual(self.run_steam().returncode, 0)

    def test_early_setup_failure_is_logged(self):
        (self.home / '.steam').write_text('not a directory')
        self.assertNotEqual(self.run_steam().returncode, 0)
        self.assertTrue((self.logs / 'steam-failed').is_file())
        self.assertIn('mkdir', (self.logs / 'steam-fedora.log').read_text())
        self.assertFalse((self.root / 'env').exists())

    def test_client_failure_is_retained(self):
        self.env['TEST_EXIT'] = '37'
        self.assertEqual(self.run_steam().returncode, 37)
        self.assertEqual((self.logs / 'steam-failed').read_text(), 'exit=37\n')
