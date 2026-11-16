import argparse
from pathlib import Path
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--kotlinc', required=True)
    parser.add_argument('--junit', required=True)
    parser.add_argument('--hamcrest', required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    main = root / 'app/src/main/java/com/droiddeck/launcher/gpu'
    tests = root / 'app/src/test/java/com/droiddeck/launcher/gpu'
    with tempfile.TemporaryDirectory(prefix='light-fg-tests-') as tmp:
        jar = Path(tmp) / 'tests.jar'
        sources = sorted(Path(__file__).with_name('framegen_light_bindings').glob('*.kt'))
        subprocess.run([args.kotlinc, *map(str, sources), str(main / 'FrameGen.kt'),
                        str(main / 'LightFrameGenPolicy.kt'), str(tests / 'LightFrameGenPolicyTest.kt'),
                        '-cp', args.junit, '-include-runtime', '-d', str(jar)], check=True)
        subprocess.run(['java', '-cp', f'{jar}:{args.junit}:{args.hamcrest}', 'org.junit.runner.JUnitCore',
                        'com.droiddeck.launcher.gpu.LightFrameGenPolicyTest',
                        'com.droiddeck.launcher.gpu.BindingsTest'], check=True)


if __name__ == '__main__':
    main()
