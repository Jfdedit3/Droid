import importlib.machinery
import importlib.util
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parents[1] / 'linuxfs/overlay/usr/local/bin/droiddeck-brave-defaults'
loader = importlib.machinery.SourceFileLoader('brave_defaults', str(SCRIPT))
spec = importlib.util.spec_from_loader(loader.name, loader)
module = importlib.util.module_from_spec(spec)
loader.exec_module(module)


class BraveDefaultsTest(unittest.TestCase):
    def test_sets_browser_but_preserves_other_associations(self):
        with tempfile.TemporaryDirectory() as tmp, patch.dict(os.environ, {'XDG_CONFIG_HOME': ''}):
            home = Path(tmp)
            config = home / '.config'
            config.mkdir()
            (config / 'mimeapps.list').write_text('[Default Applications]\ntext/html=firefox.desktop\nimage/png=viewer.desktop\n')
            (config / 'kde-mimeapps.list').write_text('[Default Applications]\ntext/html=firefox.desktop\napplication/pdf=pdf-viewer.desktop\n')
            (config / 'kdeglobals').write_text('[General]\nBrowserApplication=firefox.desktop\nColorScheme=Dark\n')
            binary = home / 'brave'
            binary.touch()
            self.assertTrue(module.configure(home, binary))
            mime = module.load(config / 'mimeapps.list')
            self.assertEqual('droiddeck-brave.desktop', mime.get('Default Applications', 'text/html'))
            self.assertEqual('viewer.desktop', mime.get('Default Applications', 'image/png'))
            kde_mime = module.load(config / 'kde-mimeapps.list')
            self.assertEqual('droiddeck-brave.desktop', kde_mime.get('Default Applications', 'text/html'))
            self.assertEqual('pdf-viewer.desktop', kde_mime.get('Default Applications', 'application/pdf'))
            kde = module.load(config / 'kdeglobals')
            self.assertEqual('droiddeck-brave.desktop', kde.get('General', 'BrowserApplication'))
            self.assertEqual('Dark', kde.get('General', 'ColorScheme'))

    def test_preserves_a_later_browser_choice(self):
        with tempfile.TemporaryDirectory() as tmp, patch.dict(os.environ, {'XDG_CONFIG_HOME': ''}):
            home = Path(tmp)
            binary = home / 'brave'
            binary.touch()
            module.configure(home, binary)
            mime = home / '.config/mimeapps.list'
            mime.write_text('[Default Applications]\ntext/html=other.desktop\n')
            self.assertFalse(module.configure(home, binary))
            self.assertIn('other.desktop', mime.read_text())

    def test_does_not_change_defaults_without_installed_browser(self):
        with tempfile.TemporaryDirectory() as tmp, patch.dict(os.environ, {'XDG_CONFIG_HOME': ''}):
            home = Path(tmp)
            self.assertFalse(module.configure(home, home / 'missing'))
            self.assertFalse((home / '.config/mimeapps.list').exists())


if __name__ == '__main__':
    unittest.main()
