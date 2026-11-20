from pathlib import Path
import sys,hashlib,zipfile
apk=Path(sys.argv[1]);root=Path(__file__).resolve().parent
assert hashlib.sha256(apk.read_bytes()).hexdigest()=="c2642d6726b03f9c925dbee10468391b242cc75738d4d7ff14df0750d1e5b3e5", "APK officiel inattendu"
with zipfile.ZipFile(apk) as z:
 for name in z.namelist():
  if name.endswith("/") or name.startswith("assets/dexopt/") or name == "assets/shortcuts.apk":continue
  if name.startswith("assets/"):target=root/"app/src/main/assets"/name.removeprefix("assets/")
  elif name.startswith("lib/"):target=root/"app/src/main/jniLibs"/name.removeprefix("lib/")
  else:continue
  target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(name))
print("Runtime et bibliothèques natives officiels récupérés")
