import argparse
import os
from pathlib import Path
import signal
import subprocess
import tempfile
import time

parser=argparse.ArgumentParser()
parser.add_argument('--qemu',required=True)
parser.add_argument('--runtime',required=True)
parser.add_argument('--payload',required=True)
args=parser.parse_args()
runtime=Path(args.runtime).resolve();payload=Path(args.payload).resolve()
command=[args.qemu,'-L',str(runtime),str(runtime/'usr/lib/ld-linux-aarch64.so.1'),
         '--library-path',str(payload/'usr/lib64')+':'+str(runtime/'usr/lib64'),
         str(payload/'usr/bin/gamescopereaper'),'--label','DroidDeck test','--']
result=subprocess.run(command+['/bin/sh','-c','printf native-child-ok'],capture_output=True,text=True,timeout=15)
assert result.returncode==0 and 'native-child-ok' in result.stdout,(result.returncode,result.stdout,result.stderr)
print('ARM64 reaper starts and waits for its child: PASS')
with tempfile.TemporaryDirectory() as directory:
    marker=Path(directory)/'child-pid'
    process=subprocess.Popen(command+['/bin/sh','-c','echo $$ > "$1"; trap "exit 0" TERM; while :; do sleep .1; done','test',str(marker)],stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,start_new_session=True)
    try:
        deadline=time.monotonic()+8
        while not marker.exists() and time.monotonic()<deadline:
            assert process.poll() is None,'reaper stopped before child startup'
            time.sleep(.05)
        assert marker.exists(),'child did not start'
        child=int(marker.read_text())
        process.terminate()
        output,error=process.communicate(timeout=10)
        assert process.returncode==0,(process.returncode,output,error)
        try:
            os.kill(child,0)
        except ProcessLookupError:
            pass
        else:
            raise AssertionError('child survived reaper shutdown')
        print('ARM64 reaper terminates its child when stopped: PASS')
    finally:
        if process.poll() is None:
            os.killpg(process.pid,signal.SIGKILL)
            process.communicate(timeout=5)
