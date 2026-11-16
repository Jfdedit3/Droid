import argparse
from pathlib import Path
import subprocess
import tempfile
p = argparse.ArgumentParser()
p.add_argument('--kotlinc', required=True)
p.add_argument('--junit', required=True)
p.add_argument('--hamcrest', required=True)
a = p.parse_args()
r = Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory() as tmp:
    jar = str(Path(tmp) / 'tests.jar')
    sources = sorted(Path(__file__).with_name('session_log_bindings').glob('*.kt'))
    subprocess.run([a.kotlinc, *map(str, sources), str(r / 'app/src/main/java/com/droiddeck/launcher/session/SessionPaths.kt'), str(r / 'app/src/test/java/com/droiddeck/launcher/session/SessionPathsTest.kt'), '-cp', a.junit, '-include-runtime', '-d', jar], check=True)
    subprocess.run(['java', '-cp', f'{jar}:{a.junit}:{a.hamcrest}', 'org.junit.runner.JUnitCore', 'com.droiddeck.launcher.session.FailureLogsTest', 'com.droiddeck.launcher.session.SessionPathsTest'], check=True)
