package com.droiddeck.launcher.core;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import java.io.*;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;

public final class RootfsArchive {
    private RootfsArchive() {}

    public static void extract(InputStream source, File root, long limit) throws IOException {
        if (!root.isDirectory() && !root.mkdirs()) throw new IOException("Cannot create rootfs");
        long written = 0;
        byte[] buffer = new byte[65536];
        try (TarArchiveInputStream tar = new TarArchiveInputStream(new GZIPInputStream(source, 65536))) {
            TarArchiveEntry e;
            while ((e = tar.getNextTarEntry()) != null) {
                String name = e.getName();
                if (name.equals(".") || name.equals("./")) continue;
                if (name.startsWith("/") || java.util.Arrays.asList(name.split("/")).contains(".."))
                    throw new IOException("Invalid rootfs path");
                File file = ArchivePaths.inside(root, name);
                if (file == null) throw new IOException("Rootfs path escapes destination");
                if (e.isDirectory()) {
                    if (!file.isDirectory() && !file.mkdirs()) throw new IOException("Cannot create " + name);
                    continue;
                }
                if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs())
                    throw new IOException("Cannot create parent of " + name);
                if (e.isSymbolicLink()) {
                    Files.createSymbolicLink(file.toPath(), new File(e.getLinkName()).toPath());
                } else if (e.isFile() && !e.isLink()) {
                    if (e.getSize() < 0 || e.getSize() > limit - written)
                        throw new IOException("Rootfs exceeds extraction limit");
                    try (OutputStream out = new FileOutputStream(file)) {
                        int n;
                        while ((n = tar.read(buffer)) != -1) {
                            written += n;
                            if (written > limit) throw new IOException("Rootfs exceeds extraction limit");
                            out.write(buffer, 0, n);
                        }
                    }
                    if ((e.getMode() & 0111) != 0 && !file.setExecutable(true, false))
                        throw new IOException("Cannot make executable: " + name);
                } else {
                    throw new IOException("Unsupported rootfs entry: " + name);
                }
            }
        }
    }
}
