import argparse
import os
from pathlib import Path
import subprocess
import tempfile
import time

p = argparse.ArgumentParser()
p.add_argument('--qemu', required=True)
p.add_argument('--root', required=True)
p.add_argument('--weston-root', required=True)
a = p.parse_args()
project = Path(__file__).resolve().parents[2]
root = Path(a.root).resolve()
weston_root = Path(a.weston_root).resolve()
preload = project / 'app/src/main/assets/linuxfs/libblsession.so'

with tempfile.TemporaryDirectory(prefix='dd-xwayland-arm64-') as tmp:
    work = Path(tmp)
    subprocess.run(['cc', str(Path(__file__).with_name('debian_link_compat') / 'deny-links.c'), '-o', str(work / 'deny-links')], check=True)
    libs = weston_root / 'usr/lib/x86_64-linux-gnu'
    env = dict(os.environ, XDG_RUNTIME_DIR=tmp, LD_LIBRARY_PATH=str(libs) + ':' + str(libs / 'weston'),
               WESTON_MODULE_MAP='headless-backend.so=' + str(libs / 'libweston-14/headless-backend.so') + ';kiosk-shell.so=' + str(libs / 'weston/kiosk-shell.so'))
    compositor = subprocess.Popen([str(weston_root / 'usr/bin/weston'), '--backend=headless', '--shell=kiosk-shell.so', '--renderer=pixman', '--width=1280', '--height=720', '--socket=wayland-0', '--idle-time=0', '--no-config', '--log=' + str(work / 'weston.log')], env=env)
    xdir = Path('/tmp/.X11-unix')
    created = not xdir.exists()
    if created:
        xdir.mkdir(mode=0o1777)
        xdir.chmod(0o1777)
    try:
        for _ in range(100):
            if (work / 'wayland-0').exists():
                break
            assert compositor.poll() is None, (work / 'weston.log').read_text()
            time.sleep(.05)
        cases = [('blocked', True, False, False, False), ('preload-without-opt-in', True, True, False, False),
                 ('lock-fixed', True, True, True, False), ('active-server-preserved', True, True, True, True),
                 ('normal-kernel', False, True, True, False)]
        for index, (name, deny, load, opt_in, existing) in enumerate(cases):
            port = str(20000 + (os.getpid() % 20000) + index)
            lock = Path('/tmp/.X' + port + '-lock')
            temp_lock = Path('/tmp/.tX' + port + '-lock')
            assert not lock.exists() and not temp_lock.exists()
            content = f'{os.getpid():10d}\n'
            if existing:
                lock.write_text(content)
            command = [str(work / 'deny-links')] if deny else []
            command += [a.qemu, '-L', str(root), '-E', 'LD_LIBRARY_PATH=' + str(root / 'lib/aarch64-linux-gnu'), '-E', 'XDG_RUNTIME_DIR=' + tmp, '-E', 'WAYLAND_DISPLAY=wayland-0']
            if load:
                command += ['-E', 'LD_PRELOAD=' + str(preload)]
            if opt_in:
                command += ['-E', 'DROIDDECK_LINK_RENAME=1']
            command += [str(root / 'usr/bin/Xwayland'), ':' + port, '-nolisten', 'tcp', '-noreset', '-shm', '-geometry', '1280x720', '-fullscreen']
            process = None
            try:
                logfile = work / (name + '.log')
                with logfile.open('w') as output:
                    process = subprocess.Popen(command, stdout=output, stderr=subprocess.STDOUT)
                    if not opt_in or existing:
                        assert process.wait(timeout=20) != 0
                        text = logfile.read_text()
                        if existing:
                            assert 'already active' in text, text
                            assert lock.read_text() == content
                        else:
                            assert 'Linking lock file' in text and 'Operation not permitted' in text, text
                    else:
                        for _ in range(150):
                            assert process.poll() is None, logfile.read_text()
                            ready = subprocess.run(['xdpyinfo', '-display', ':' + port], capture_output=True, text=True)
                            if ready.returncode == 0:
                                assert '1280x720' in ready.stdout
                                break
                            time.sleep(.1)
                        else:
                            raise AssertionError('X11 readiness timeout: ' + logfile.read_text())
                print(name + ': PASS', flush=True)
            finally:
                if process and process.poll() is None:
                    process.terminate()
                    try:
                        process.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait()
                for path in [lock, temp_lock, xdir / ('X' + port)]:
                    if path.exists() or path.is_symlink():
                        path.unlink()
    finally:
        compositor.terminate()
        try:
            compositor.wait(timeout=5)
        except subprocess.TimeoutExpired:
            compositor.kill()
            compositor.wait()
        if created:
            try:
                xdir.rmdir()
            except OSError:
                pass
print('5 actual ARM64 Xwayland regressions passed; 1280x720 X11 server verified on headless Wayland')
