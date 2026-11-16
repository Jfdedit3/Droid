from pathlib import Path
import sys,hashlib,zipfile
apk=Path(sys.argv[1]);root=Path(__file__).resolve().parent
assert hashlib.sha256(apk.read_bytes()).hexdigest()=="179ab8b4dc8f9223b70a1e4287c55a090b36ee376242186a60211100158462f1", "APK officiel inattendu"
with zipfile.ZipFile(apk) as z:
 for name in z.namelist():
  if name.endswith("/") or name.startswith("assets/dexopt/"):continue
  if name.startswith("assets/"):target=root/"app/src/main/assets"/name.removeprefix("assets/")
  elif name.startswith("lib/"):target=root/"app/src/main/jniLibs"/name.removeprefix("lib/")
  else:continue
  target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(name))
print("Runtime et bibliothèques natives officiels récupérés")
