package com.droiddeck.launcher.core;

import java.io.*;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.tukaani.xz.XZInputStream;

public final class BravePayload {
    private static final String PREFIX = "opt/brave.com/brave/";
    private BravePayload() {}

    public static void extract(InputStream source, File destination) throws IOException {
        try (ArArchiveInputStream deb = new ArArchiveInputStream(new BufferedInputStream(source, 1 << 16))) {
            ArArchiveEntry member;
            while ((member = deb.getNextArEntry()) != null) {
                if (!member.getName().equals("data.tar.xz")) continue;
                try (TarArchiveInputStream tar = new TarArchiveInputStream(new XZInputStream(deb))) {
                    TarArchiveEntry entry;
                    while ((entry = tar.getNextTarEntry()) != null) {
                        String name = entry.getName();
                        while (name.startsWith("./")) name = name.substring(2);
                        if (!name.startsWith(PREFIX)) continue;
                        String relative = name.substring(PREFIX.length());
                        File file = ArchivePaths.inside(destination, relative);
                        if (file == null) throw new IOException("Browser archive entry escapes its directory");
                        if (entry.isDirectory()) {
                            if (!file.isDirectory() && !file.mkdirs()) throw new IOException("Cannot create browser directory");
                        } else if (entry.isFile()) {
                            File parent = file.getParentFile();
                            if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create browser parent");
                            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file), 1 << 16)) {
                                byte[] buffer = new byte[1 << 16];
                                for (int n; (n = tar.read(buffer)) >= 0;) if (n > 0) out.write(buffer, 0, n);
                            }
                            if ((entry.getMode() & 0111) != 0 && !file.setExecutable(true, false))
                                throw new IOException("Cannot make browser executable");
                        } else {
                            throw new IOException("Unexpected browser archive entry type");
                        }
                    }
                }
                if (!new File(destination, "brave").isFile() || !new File(destination, "resources.pak").isFile())
                    throw new IOException("Incomplete browser payload");
                return;
            }
        }
        throw new IOException("Browser package has no supported data archive");
    }
}
