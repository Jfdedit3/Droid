import unittest
from test_game_environment import MODULE


class SoftwareUiIsolationTests(unittest.TestCase):
    def environment(self, distro):
        return {"DROIDDECK_" + distro + "_SOFTWARE_UI": "1",
                "DROIDDECK_" + distro.removesuffix("_STEAM") + "_GAME_GALLIUM_DRIVER": "zink",
                "GALLIUM_DRIVER": "llvmpipe", "LIBGL_ALWAYS_SOFTWARE": "1",
                "DROIDDECK_GAME_MESA_NO_ERROR": "0", "MESA_NO_ERROR": "1",
                "VK_DRIVER_FILES": "/manual-radv.json"}

    def test_games_restore_radv_and_gpu_rendering_for_both_desktops(self):
        for distro in ("FEDORA", "UBUNTU_STEAM"):
            with self.subTest(distro=distro):
                original = self.environment(distro)
                result = MODULE["game_gpu"](original)
                self.assertEqual(result["GALLIUM_DRIVER"], "zink")
                self.assertEqual(result["VK_DRIVER_FILES"], "/manual-radv.json")
                self.assertEqual(result["MESA_NO_ERROR"], "0")
                self.assertNotIn("LIBGL_ALWAYS_SOFTWARE", result)
                self.assertFalse(any(k.startswith("DROIDDECK_" + distro) for k in result))
                self.assertEqual(original["GALLIUM_DRIVER"], "llvmpipe")

    def test_chromium_helpers_keep_software_ui_and_do_not_get_game_overrides(self):
        for distro in ("FEDORA", "UBUNTU_STEAM"):
            with self.subTest(distro=distro):
                result = MODULE["game_gpu"](self.environment(distro), keep=True)
                self.assertEqual(result["GALLIUM_DRIVER"], "llvmpipe")
                self.assertEqual(result["LIBGL_ALWAYS_SOFTWARE"], "1")
                self.assertEqual(result["MESA_NO_ERROR"], "1")
