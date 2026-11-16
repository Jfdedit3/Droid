#!/bin/sh
set -eu
root=$1
proot=$2
qemu=$3
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/root" "$work/usr/lib64"
ln -s usr/lib "$work/lib"
ln -s usr/lib64 "$work/lib64"
"$proot" -q "$qemu" -r "$work" -b /dev -b /proc \
  -b "$root/usr/lib64:/usr/lib/aarch64-linux-gnu" \
  -b "$root/usr/lib/ld-linux-aarch64.so.1:/usr/lib/ld-linux-aarch64.so.1" \
  -b "$root/usr/bin/env:/run/droiddeck-fex/bin/env" \
  -b "$root/usr/bin/uname:/run/droiddeck-fex/bin/probe" -w /root \
  /usr/lib/ld-linux-aarch64.so.1 --library-path /usr/lib/aarch64-linux-gnu \
  /run/droiddeck-fex/bin/env -i LD_LIBRARY_PATH=/usr/lib/aarch64-linux-gnu \
  /run/droiddeck-fex/bin/probe -m | tee "$work/output"
grep -qx aarch64 "$work/output"
