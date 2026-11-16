import importlib.machinery
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

SOURCE = Path(__file__).resolve().parents[1] / 'linuxfs/overlay/usr/local/bin/droiddeck-ubuntu-windows'
loader = importlib.machinery.SourceFileLoader('ubuntu_windows', str(SOURCE))
spec = importlib.util.spec_from_loader(loader.name, loader)
profile = importlib.util.module_from_spec(spec)
loader.exec_module(profile)


class UbuntuAppearanceTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.home = self.root / 'home'
        self.config = self.home / '.config'
        self.config.mkdir(parents=True)
        self.release = self.root / 'os-release'
        self.release.write_text('ID=ubuntu\n')
        self.assets = self.root / 'assets'
        self.assets.mkdir()
        (self.assets / 'windows-layout.js').write_text('layout-script')
        (self.assets / 'windows-wallpaper.png').write_bytes(b'wallpaper')
        self.updates = self.root / 'shell/contents/updates'
        self.updates.parent.mkdir(parents=True)

    def apply(self, restore=False):
        return profile.apply(self.home, self.release, self.assets, self.updates, restore)

    def test_existing_preferences_and_desktop_widgets_are_preserved_in_backup(self):
        previous = '[General]\nColorScheme=Vapor\ncustomSetting=keep\n'
        (self.config / 'kdeglobals').write_text(previous)
        layout = self.config / 'plasma-org.kde.plasma.desktop-appletsrc'
        layout.write_text('my desktop widgets')
        self.apply()
        self.assertEqual(layout.read_text(), 'my desktop widgets')
        self.assertIn('customSetting=keep', (self.config / 'kdeglobals').read_text())
        backup = self.home / '.local/share/droiddeck/ubuntu-windows-v1/original/kdeglobals'
        self.assertEqual(backup.read_text(), previous)
        self.assertEqual((self.updates / profile.UPDATE_NAME).read_text(), 'layout-script')

    def test_does_not_reapply_over_later_customizations(self):
        self.apply()
        (self.config / 'kdeglobals').write_text('personal colors')
        self.assertEqual(self.apply(), 'already configured')
        self.assertEqual((self.config / 'kdeglobals').read_text(), 'personal colors')

    def test_restores_original_files_and_removes_only_created_configuration(self):
        (self.config / 'kdeglobals').write_text('original colors')
        (self.config / 'unrelated').write_text('keep me')
        self.apply()
        self.apply(restore=True)
        self.assertEqual((self.config / 'kdeglobals').read_text(), 'original colors')
        self.assertFalse((self.config / 'kwinrc').exists())
        self.assertEqual((self.config / 'unrelated').read_text(), 'keep me')
        self.assertFalse((self.updates / profile.UPDATE_NAME).exists())
        self.assertEqual(self.apply(), 'already configured')

    def test_fedora_is_untouched_even_if_helper_is_called(self):
        self.release.write_text('ID=fedora\n')
        (self.config / 'kdeglobals').write_text('Fedora preferences')
        self.assertEqual(self.apply(), 'skipped: not Ubuntu')
        self.assertEqual((self.config / 'kdeglobals').read_text(), 'Fedora preferences')
        self.assertFalse((self.home / '.local').exists())
        self.assertFalse(self.updates.exists())

    def test_broken_configuration_symlink_is_restored(self):
        (self.config / 'kdeglobals').symlink_to('missing-original')
        self.apply()
        self.apply(restore=True)
        self.assertTrue((self.config / 'kdeglobals').is_symlink())
        self.assertEqual((self.config / 'kdeglobals').readlink().as_posix(), 'missing-original')

    def test_missing_assets_fail_before_modifying_preferences(self):
        (self.config / 'kdeglobals').write_text('untouched')
        (self.assets / 'windows-layout.js').unlink()
        with self.assertRaises(OSError):
            self.apply()
        self.assertEqual((self.config / 'kdeglobals').read_text(), 'untouched')
        self.assertFalse((self.home / '.local').exists())
