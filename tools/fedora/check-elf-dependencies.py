#!/usr/bin/python3
import os
from pathlib import Path
import struct
import sys

steam = Path(os.environ.get('HOME', '/root')) / '.local/share/Steam/steamrtarm64'
search = [steam, steam / 'panorama', Path('/usr/lib64'), Path('/usr/lib'), Path('/usr/local/lib'), Path('/usr/lib64/pulseaudio')]


def needed(path):
    with path.open('rb') as f:
        header = f.read(64)
        if header[:6] != b'\x7fELF\x02\x01' or header[18:20] != b'\xb7\x00':
            return None
        shoff = struct.unpack_from('<Q', header, 40)[0]
        size, count = struct.unpack_from('<HH', header, 58)
        f.seek(shoff)
        sections = [struct.unpack('<IIQQQQIIQQ', f.read(size)) for _ in range(count)]
        result = []
        for section in sections:
            if section[1] != 6:
                continue
            strings = sections[section[6]]
            f.seek(strings[4])
            strings = f.read(strings[5])
            f.seek(section[4])
            for _ in range(section[5] // 16):
                tag, index = struct.unpack('<qQ', f.read(16))
                if tag == 1:
                    result.append(strings[index:].split(b'\0', 1)[0].decode())
        return result


pending = [p for folder in [steam, steam / 'panorama'] if folder.is_dir() for p in folder.iterdir() if p.is_file()]
seen, missing = set(), set()
checked = 0
while pending:
    path = pending.pop()
    if not path.is_file() or path in seen:
        continue
    seen.add(path)
    dependencies = needed(path)
    if dependencies is None:
        continue
    checked += 1
    for name in dependencies:
        resolved = next((folder / name for folder in search if (folder / name).is_file()), None)
        if resolved is None:
            missing.add(name)
        else:
            pending.append(resolved)
print('Checked', checked, 'ARM64 ELF files')
print('Missing:', ', '.join(sorted(missing)) or 'none')
sys.exit(bool(missing))
