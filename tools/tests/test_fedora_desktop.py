import os
from pathlib import Path
import signal
import socket
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'linuxfs/overlay/usr/local/bin/droiddeck-fedora-xfce'


class FedoraLauncherTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        (self.root / 'bin').mkdir()
        self.home = self.root / 'home'
        self.home.mkdir()
        self.env = dict(os.environ, HOME=str(self.home), MOCK_DIR=str(self.root), PATH=str(self.root / 'bin') + ':' + os.environ['PATH'])
        self.env.update(GALLIUM_DRIVER='zink', MESA_LOADER_DRIVER_OVERRIDE='zink', LIBGL_KOPPER_DRI2='true')
        self.env['BL_WIDTH'] = '1280'
        self.env['BL_HEIGHT'] = '720'
        self.env['WAYLAND_DISPLAY'] = 'wayland-0'
        self.env['XDG_RUNTIME_DIR'] = str(self.root)
        self.env['BL_DEBUG_DIR'] = str(self.root)
        self.socket = socket.socket(socket.AF_UNIX)
        self.socket.bind(str(self.root / 'wayland-0'))
        self.addCleanup(self.socket.close)
        for name, body in {
            'Xwayland': 'env >"$MOCK_DIR/xwayland-env"; echo MOCK_XWAYLAND_ERROR >&2; echo "$$" >"$MOCK_DIR/pid"; printf "%s\\n" "$@" >"$MOCK_DIR/args"; [ "${MOCK_FAIL:-0}" = 0 ] || exit 37; touch "$MOCK_DIR/ready"; trap "exit 0" TERM; while :; do sleep 0.05; done',
            'python3': 'printf "%s\\n" "$*" >>"$MOCK_DIR/python-args"; exit 0',
            'xdpyinfo': 'test -e "$MOCK_DIR/ready"',
            'dbus-run-session': 'env >"$MOCK_DIR/session-env"; exit "${MOCK_STATUS:-0}"',
        }.items():
            p = self.root / 'bin' / name
            p.write_text('#!/bin/sh\n' + body + '\n')
            p.chmod(0o755)
        text = SCRIPT.read_text().replace('/tmp/.X11-unix', str(self.root / 'x11')).replace('/tmp/.ICE-unix', str(self.root / 'ice')).replace('/tmp/.X1-lock', str(self.root / 'lock'))
        text = text.replace('/etc/machine-id', str(self.root / 'machine-id')).replace('/var/lib/dbus', str(self.root / 'dbus'))
        self.script = self.root / 'launcher'
        self.script.write_text(text)

    def run_launcher(self):
        return subprocess.run(['sh', str(self.script)], env=self.env, capture_output=True, text=True, timeout=20)

    def assert_server_stopped(self):
        pid = int((self.root / 'pid').read_text())
        with self.assertRaises(ProcessLookupError):
            os.kill(pid, 0)

    def test_lightweight_defaults_and_brave(self):
        result = self.run_launcher()
        self.assertEqual(result.returncode, 0, result.stderr)
        config = self.home / '.config/xfce4/xfconf/xfce-perchannel-xml/xfwm4.xml'
        self.assertIn('name="use_compositing" type="bool" value="false"', config.read_text())
        self.assertIn('droiddeck-brave.desktop', (self.home / '.config/mimeapps.list').read_text())
        self.assertIn('Hidden=true', (self.home / '.config/autostart/xfce-polkit.desktop').read_text())
        self.assertIn('-shm', (self.root / 'args').read_text())
        xenv = (self.root / 'xwayland-env').read_text()
        self.assertIn('DROIDDECK_LINK_RENAME=1', xenv)
        self.assertIn('XWAYLAND_NO_GLAMOR=1', xenv)
        self.assertIn('LD_PRELOAD=/usr/local/lib/libblsession.so', xenv)
        self.assertIn('1280x720', (self.root / 'args').read_text())
        env = (self.root / 'session-env').read_text()
        self.assertIn('DISPLAY=:1', env)
        self.assertIn('BL_DESKTOP=1', env)
        self.assertIn('GALLIUM_DRIVER=llvmpipe', env)
        self.assertIn('LIBGL_ALWAYS_SOFTWARE=1', env)
        self.assertNotIn('\nMESA_LOADER_DRIVER_OVERRIDE=', env)
        self.assertNotIn('\nLIBGL_KOPPER_DRI2=', env)
        self.assertNotIn('WAYLAND_DISPLAY=', env)
        self.assertNotIn('LD_PRELOAD=', env)
        self.assertNotIn('DROIDDECK_LINK_RENAME=', env)
        self.assertNotIn('XWAYLAND_NO_GLAMOR=', env)
        self.assert_server_stopped()

    def test_does_not_overwrite_later_user_preferences(self):
        self.assertEqual(self.run_launcher().returncode, 0)
        config = self.home / '.config/mimeapps.list'
        config.write_text('my-browser')
        xfwm = self.home / '.config/xfce4/xfconf/xfce-perchannel-xml/xfwm4.xml'
        xfwm.write_text('my-settings')
        self.assertEqual(self.run_launcher().returncode, 0)
        self.assertEqual(config.read_text(), 'my-browser')
        self.assertEqual(xfwm.read_text(), 'my-settings')

    def test_preserves_session_failure_and_cleans_display(self):
        self.env['MOCK_STATUS'] = '17'
        self.assertEqual(self.run_launcher().returncode, 17)
        self.assert_server_stopped()

    def test_display_failure_prevents_session_start(self):
        self.env['MOCK_FAIL'] = '1'
        self.assertEqual(self.run_launcher().returncode, 70)
        self.assertFalse((self.root / 'session-env').exists())
        self.assert_server_stopped()
        self.assertIn('status 37', (self.root / 'fedora-xfce.log').read_text())
        self.assertIn('MOCK_XWAYLAND_ERROR', (self.root / 'xwayland-fedora.log').read_text())


if __name__ == '__main__':
    unittest.main()
