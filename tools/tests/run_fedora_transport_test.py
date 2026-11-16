from pathlib import Path
import os
import subprocess
import sys
import tempfile

from test_netlink_preload import NetlinkPreloadTest

NetlinkPreloadTest.setUpClass()
try:
    helper = Path(__file__).resolve().parents[1] / 'linuxfs/overlay/usr/local/bin/droiddeck-fedora-tools'
    program = '''import os,socket,subprocess,sys
s=socket.socket();s.bind(('127.0.0.1',0));s.listen()
port=s.getsockname()[1]
env=dict(os.environ);env.pop('LD_PRELOAD',None)
v=subprocess.run([sys.executable,sys.argv[1],'--lsof','-nP','-F','pRu','-iTCP@127.0.0.1:'+str(port)],env=env,capture_output=True,text=True)
assert v.returncode==0,(v.returncode,v.stderr,v.stdout)
assert v.stdout=='p%d\\nR%d\\nu%d\\nn127.0.0.1:%d\\n'%(os.getpid(),os.getppid(),os.getuid(),port),v.stdout
print('Real loopback socket owner identified after child clears LD_PRELOAD: PASS')
'''
    with tempfile.TemporaryDirectory() as directory:
        env=dict(os.environ,LD_PRELOAD=str(NetlinkPreloadTest.preload),BL_SYSV_DIR=directory)
        subprocess.run([sys.executable,'-c',program,str(helper)],env=env,check=True,timeout=20)
        if len(sys.argv) > 1:
            root = Path(directory) / 'guest'
            etc = root / 'etc'
            etc.mkdir(parents=True)
            preload = etc / 'ld.so.preload'
            preload.write_text(str(NetlinkPreloadTest.preload) + '\n')
            env.pop('LD_PRELOAD', None)
            env['PROOT_NO_SECCOMP'] = '1'
            if len(sys.argv) > 2:
                env['LD_LIBRARY_PATH'] = sys.argv[2]
            command = [sys.argv[1], '-r', str(root), '-w', '/tmp']
            for path in ('/usr', '/lib', '/lib64', '/bin', '/dev', '/proc', '/opt', '/workspace', '/tmp'):
                if Path(path).exists():
                    command += ['-b', path]
            subprocess.run(command + [sys.executable, '-c', "assert 'LD_PRELOAD' not in __import__('os').environ\n" + program,
                            str(helper)], env=env, check=True, timeout=30)
            print('System loader hooks survive cleared LD_PRELOAD under PRoot: PASS')
finally:
    NetlinkPreloadTest.tearDownClass()
