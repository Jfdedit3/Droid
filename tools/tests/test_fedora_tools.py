import importlib.machinery
import importlib.util
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'tools/linuxfs/overlay/usr/local/bin/droiddeck-fedora-tools'
loader = importlib.machinery.SourceFileLoader('fedora_tools', str(SOURCE))
spec = importlib.util.spec_from_loader(loader.name, loader)
tools = importlib.util.module_from_spec(spec)
loader.exec_module(tools)


class DesktopIntegrationTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.home = self.root / 'home'
        self.assets = self.root / 'assets'
        self.apps = self.root / 'applications'
        self.home.mkdir()
        self.assets.mkdir()
        (self.assets / 'fedora-wallpaper.png').write_bytes(b'wallpaper')
        self.config = self.home / '.config/xfce4/xfconf/xfce-perchannel-xml/xfce4-desktop.xml'

    def setup(self):
        tools.setup(self.home, self.assets, self.apps, {'0', 'XWAYLAND0'})

    def test_background_and_real_shortcuts_are_created(self):
        self.setup()
        channel = ET.parse(self.config).getroot()
        p = channel.find("./property[@name='backdrop']/property[@name='screen0']/property[@name='monitorXWAYLAND0']/property[@name='workspace0']/property[@name='last-image']")
        self.assertEqual(p.get('value'), str(self.assets / 'fedora-wallpaper.png'))
        desktop = self.home / 'Desktop'
        self.assertEqual(len(list(desktop.glob('*.desktop'))), 5)
        self.assertTrue(os.access(desktop / 'DroidDeck-Steam.desktop', os.X_OK))
        self.assertTrue((self.apps / 'droiddeck-brave.desktop').exists())
        self.assertFalse((self.apps / 'DroidDeck-Brave.desktop').exists())
        self.assertIn('droiddeck-fedora-tools steam', (desktop / 'DroidDeck-Steam.desktop').read_text())
        self.assertIn('Steam', (desktop / 'DroidDeck-Steam.desktop').read_text())
        self.assertIn('droiddeck-brave-browser', (desktop / 'DroidDeck-Brave.desktop').read_text())
        self.assertIn('thunar /mnt/shared', (desktop / 'DroidDeck-Shared.desktop').read_text())

    def test_existing_personal_wallpaper_is_preserved(self):
        image = self.root / 'my photo.png'
        image.write_bytes(b'image')
        self.config.parent.mkdir(parents=True)
        self.config.write_text('<channel name="xfce4-desktop" version="1.0"><property name="backdrop" type="empty"><property name="screen0" type="empty"><property name="monitorXWAYLAND0" type="empty"><property name="workspace0" type="empty"><property name="last-image" type="string" value="' + str(image) + '"/><property name="image-style" type="int" value="5"/></property></property></property></property></channel>')
        self.setup()
        self.assertIn(str(image), self.config.read_text())
        monitor = ET.parse(self.config).getroot().find("./property[@name='backdrop']/property[@name='screen0']/property[@name='monitorXWAYLAND0']")
        self.assertNotIn(str(self.assets / 'fedora-wallpaper.png'), ET.tostring(monitor, encoding='unicode'))

    def test_stock_svg_background_is_replaced_even_when_its_file_exists(self):
        stock = '/usr/share/backgrounds/xfce/xfce-verticals.svg'
        self.config.parent.mkdir(parents=True)
        self.config.write_text('<channel name="xfce4-desktop" version="1.0"><property name="backdrop"><property name="screen0"><property name="monitorXWAYLAND0"><property name="workspace0"><property name="last-image" value="' + stock + '"/><property name="image-style" type="int" value="5"/></property></property></property></property></channel>')
        original = Path.is_file
        with patch.object(Path, 'is_file', lambda path: str(path) == stock or original(path)):
            self.setup()
        monitor = ET.parse(self.config).getroot().find("./property[@name='backdrop']/property[@name='screen0']/property[@name='monitorXWAYLAND0']")
        self.assertNotIn(stock, ET.tostring(monitor, encoding='unicode'))
        self.assertIn(str(self.assets / 'fedora-wallpaper.png'), ET.tostring(monitor, encoding='unicode'))

    def test_later_changes_and_deleted_shortcuts_stay_unchanged(self):
        self.setup()
        self.config.write_text('user preferences')
        desktop = self.home / 'Desktop'
        (desktop / 'DroidDeck-Steam.desktop').unlink()
        brave = desktop / 'DroidDeck-Brave.desktop'
        brave.write_text('my custom launcher')
        self.setup()
        self.assertEqual(self.config.read_text(), 'user preferences')
        self.assertEqual(brave.read_text(), 'my custom launcher')
        self.assertFalse((desktop / 'DroidDeck-Steam.desktop').exists())
        self.assertTrue((self.apps / 'DroidDeck-Steam.desktop').exists())

    def test_disabled_black_background_is_migrated(self):
        image = self.root / 'old.png'
        image.write_bytes(b'image')
        self.config.parent.mkdir(parents=True)
        self.config.write_text('<channel name="xfce4-desktop" version="1.0"><property name="backdrop"><property name="screen0"><property name="monitorXWAYLAND0"><property name="workspace0"><property name="last-image" value="' + str(image) + '"/><property name="image-style" type="int" value="0"/></property></property></property></property></channel>')
        self.setup()
        after = self.config.read_text().split('monitorXWAYLAND0')[1]
        self.assertIn(str(self.assets / 'fedora-wallpaper.png'), after)
        self.assertIn('name="image-style" type="int" value="5"', after)

    def test_localized_desktop_directory_is_used(self):
        path = self.home / '.config/user-dirs.dirs'
        path.parent.mkdir()
        path.write_text('XDG_DESKTOP_DIR="$HOME/Bureau"\n')
        self.setup()
        self.assertTrue((self.home / 'Bureau/DroidDeck-Steam.desktop').is_file())
        self.assertFalse((self.home / 'Desktop').exists())

    def test_steam_executes_in_this_desktop_without_a_relaunch_request(self):
        with patch.dict(os.environ, DD_LAUNCH_DIR=str(self.root)), patch.object(tools.os, 'execv') as execute:
            tools.launch('steam', 'steam://rungameid/123')
        execute.assert_called_once_with('/usr/libexec/droiddeck-fedora/droiddeck-fedora-steam',
            ['droiddeck-fedora-steam', 'steam://rungameid/123'])
        self.assertFalse((self.root / 'steam-launch').exists())

    def test_ubuntu_selects_the_existing_plasma_mode(self):
        with patch.dict(os.environ, DD_LAUNCH_DIR=str(self.root)):
            tools.launch('ubuntu')
        self.assertEqual((self.root / 'steam-launch').read_text(), 'ui=desktop\nurl=\nsession=desktop\n')

    def test_invalid_requests_do_not_replace_the_mailbox(self):
        request = self.root / 'steam-launch'
        request.write_text('previous')
        with patch.dict(os.environ, DD_LAUNCH_DIR=str(self.root)):
            for mode, url in [('unknown', ''), ('steam', 'https://example.test'), ('steam', 'steam://foo\nsession=desktop'), ('steam', 'steam://foo\x00')]:
                with self.subTest(mode=mode, url=url), self.assertRaises(ValueError):
                    tools.launch(mode, url)
        self.assertEqual(request.read_text(), 'previous')

    def test_steam_does_not_need_the_session_switch_channel(self):
        with patch.dict(os.environ, DD_LAUNCH_DIR=''), patch.object(tools.os, 'execv') as execute:
            tools.launch('steam')
        self.assertTrue(execute.called)

    def test_malformed_user_xml_is_not_overwritten(self):
        self.config.parent.mkdir(parents=True)
        self.config.write_text('<broken')
        with self.assertRaises(ET.ParseError):
            self.setup()
        self.assertEqual(self.config.read_text(), '<broken')
        self.assertFalse((self.home / '.config' / tools.MARKER).exists())

    def test_monitor_detection_has_a_safe_fallback(self):
        with patch.object(tools.ctypes, 'CDLL', side_effect=OSError):
            self.assertEqual(tools.monitors(), {'0', 'XWAYLAND0'})

    def test_bundled_graphics_are_small_png_files(self):
        from PIL import Image
        assets = ROOT / 'tools/linuxfs/overlay/usr/local/share/droiddeck'
        with Image.open(assets / 'fedora-wallpaper.png') as image:
            self.assertEqual(image.size, (1280, 720))
            self.assertNotEqual(image.getpixel((300, 300)), (0, 0, 0))
        with Image.open(assets / 'fedora-steam.png') as image:
            self.assertEqual(image.size, (128, 128))
        self.assertLess(sum(p.stat().st_size for p in assets.glob('fedora-*.png')), 100000)


if __name__ == '__main__':
    unittest.main()

class SteamTransportTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / 'net').mkdir()

    def query(self, data, *args):
        import contextlib
        import io
        if data is not None:
            (self.root / 'net/p43210').write_text(data)
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            status = tools.steam_lsof(args or ['-nP', '-F', 'pRu', '-iTCP@127.0.0.1:43210'], {'DD_SYSV_DIR': str(self.root)})
        return status, output.getvalue()

    def test_connected_socket_uses_upstream_lsof_protocol(self):
        self.assertEqual(self.query('123 99 1000 50000 8 999 2\n'),
                         (0, 'p123\nR99\nu1000\nn127.0.0.1:43210->127.0.0.1:50000\n'))

    def test_listener_uses_upstream_lsof_protocol(self):
        self.assertEqual(self.query('123 99 1000 0\n'), (0, 'p123\nR99\nu1000\nn127.0.0.1:43210\n'))

    def test_missing_or_invalid_records_fail_closed(self):
        for data in [None, '', 'garbage', '0 1 1000 0', '123 1 1000 99999']:
            self.assertEqual(self.query(data), (1, ''))

    def test_queries_are_limited_to_loopback_ports(self):
        for arg in ['-iTCP@192.168.1.1:43210', '-iTCP@127.0.0.1:0', '-iTCP@127.0.0.1:65536', '/etc/passwd']:
            self.assertEqual(self.query('123 99 1000 0\n', arg), (1, ''))

    def test_install_is_idempotent_and_keeps_existing_preloads(self):
        preload = self.root / 'etc/ld.so.preload'
        preload.parent.mkdir()
        preload.write_text('/custom/library.so\n')
        lsof = self.root / 'bin/lsof'
        tools.install_transport(lsof, preload)
        tools.install_transport(lsof, preload)
        self.assertEqual(preload.read_text(), '/custom/library.so\n/usr/local/lib/libddsession.so\n/usr/local/lib/libfakeinput.so\n')
        self.assertTrue(os.access(lsof, os.X_OK))
        self.assertIn('--lsof', lsof.read_text())

    def test_existing_lsof_is_preserved(self):
        lsof = self.root / 'lsof'
        lsof.write_text('existing executable')
        tools.install_transport(lsof, self.root / 'preload')
        self.assertEqual(lsof.read_text(), 'existing executable')
