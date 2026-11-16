#!/usr/bin/env python3
"""Reconstitute the pinned Fedora image from the repository's Git-sized parts."""
from pathlib import Path
import hashlib
import json

root=Path(__file__).resolve().parents[2]
folder=root/'build-inputs/fedora-base'
manifest=json.loads((folder/'parts.json').read_text())
expected=json.loads((root/'tools/fedora/manifest.json').read_text())
assert manifest['sha256']==expected['sha256'] and manifest['bytes']==expected['bytes']
destination=root/'app/src/main/assets/bundled'/manifest['file']

def digest(path):
    value=hashlib.sha256()
    with path.open('rb') as stream:
        for data in iter(lambda:stream.read(1024*1024),b''):
            value.update(data)
    return value.hexdigest()

if destination.is_file() and digest(destination)==manifest['sha256']:
    print('Fedora image already present and verified')
else:
    destination.parent.mkdir(parents=True,exist_ok=True)
    temporary=destination.with_suffix(destination.suffix+'.tmp')
    try:
        total=0
        with temporary.open('wb') as output:
            for part in manifest['parts']:
                path=folder/part['file']
                if path.stat().st_size!=part['bytes'] or digest(path)!=part['sha256']:
                    raise ValueError('Invalid image part: '+part['file'])
                with path.open('rb') as stream:
                    for data in iter(lambda:stream.read(1024*1024),b''):
                        output.write(data)
                        total+=len(data)
        if total!=manifest['bytes'] or digest(temporary)!=manifest['sha256']:
            raise ValueError('Invalid reconstructed Fedora image')
        temporary.replace(destination)
        print('Fedora image reconstituted and SHA-256 verified:',destination)
    finally:
        if temporary.exists():temporary.unlink()
