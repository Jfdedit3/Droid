from pathlib import Path
import tempfile,subprocess,os
r=Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory() as d:
 p=Path(d); src=(r/'tools/linuxfs/preload/links.c').read_text().replace('#if __SIZEOF_POINTER__ != 8','#if 1'); (p/'links.c').write_text(src)
 subprocess.run(['cc','-shared','-fPIC',str(p/'links.c'),'-ldl','-o',str(p/'links.so')],check=True)
 subprocess.run(['cc',str(r/'tools/tests/debian_link_compat/deny-links.c'),'-o',str(p/'deny')],check=True)
 for enabled in [False,True]:
  target=p/('ok' if enabled else 'blocked');env=dict(os.environ,ICEAUTHORITY=str(target))
  if enabled:env.update(LD_PRELOAD=str(p/'links.so'),DROIDDECK_LINK_RENAME='1')
  v=subprocess.run([str(p/'deny'),'/usr/bin/iceauth','add','ICE','','local/test','MIT-MAGIC-COOKIE-1','00112233445566778899aabbccddeeff'],env=env,capture_output=True,text=True,timeout=30)
  print('shim='+str(enabled),'exit='+str(v.returncode),v.stderr.strip())
  assert (v.returncode==0)==enabled
  if enabled:
   listing=subprocess.run(['/usr/bin/iceauth','-f',str(target),'list'],capture_output=True,text=True,check=True)
   assert '00112233445566778899aabbccddeeff' in listing.stdout
   (p/'ok-l').write_text('existing-owner'); before=target.read_bytes()
   v=subprocess.run([str(p/'deny'),'/usr/bin/iceauth','add','ICE','','local/other','MIT-MAGIC-COOKIE-1','aa'],env=env,capture_output=True,text=True,timeout=30)
   assert v.returncode!=0 and (p/'ok-l').read_text()=='existing-owner' and target.read_bytes()==before
   print('active lock preserved: PASS')
print('Actual iceauth under denied link/linkat: PASS')
