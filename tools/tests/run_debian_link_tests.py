import argparse
from pathlib import Path
import os
import subprocess
import tempfile

p = argparse.ArgumentParser()
p.add_argument('--proot', required=True)
a = p.parse_args()
sources = Path(__file__).with_name('debian_link_compat')
with tempfile.TemporaryDirectory(prefix='debian-lock-test-') as work:
    work = Path(work)
    for name in ['deny-links', 'lock-probe']:
        subprocess.run(['cc', str(sources / (name + '.c')), '-o', str(work / name)], check=True)
    for name, compatible, no_seccomp in [('blocked-without-compatibility', False, True), ('blocked-with-seccomp-acceleration', True, False), ('compatible-full-tracing', True, True)]:
        root = work / name
        (root / 'tmp').mkdir(parents=True)
        links = root / '.proot-l2s'
        links.mkdir()
        env = os.environ.copy()
        env['PROOT_L2S_DIR'] = str(links)
        env['PROOT_TMP_DIR'] = str(work)
        env.pop('PROOT_NO_SECCOMP', None)
        if no_seccomp:
            env['PROOT_NO_SECCOMP'] = '1'
        command = [str(work / 'deny-links'), a.proot]
        if compatible:
            command += ['--link2symlink']
        command += ['-r', str(root), '-w', '/tmp']
        for path in ['/usr', '/lib', '/lib64', '/bin', '/dev', '/proc']:
            if Path(path).exists():
                command += ['-b', path]
        command += ['-b', str(work / 'lock-probe') + ':/probe', '/probe', '/tmp']
        result = subprocess.run(command, env=env, capture_output=True, text=True, timeout=20)
        if compatible and no_seccomp:
            assert result.returncode == 0, (name, result.stdout, result.stderr)
            assert 'survived unlink' in result.stdout
            assert not (root / 'tmp/.X1-lock').exists()
        else:
            assert result.returncode == 1 and 'Operation not permitted' in result.stderr, (result.returncode, result.stderr)
        print(name + ': PASS')
print('3 hard-link compatibility regressions passed under a kernel filter denying hard links')
