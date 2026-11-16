package com.droiddeck.launcher.core;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

public final class FedoraGamingPayload {
    private static final String MARKER = ".droiddeck-gaming-libs";
    private static final String[] LIBRARIES = {"libwayland-server.so.0", "libseat.so.1", "libinput.so.10", "libavif.so.16", "libeis.so.1", "libluajit-5.1.so.2", "libstdc++.so.6.0.35"};
    private FedoraGamingPayload() {}

    public static boolean ready(File root, String hash) {
        try {
            if (!new String(Files.readAllBytes(new File(root, MARKER).toPath()), "UTF-8").equals(hash)) return false;
            for (String name : LIBRARIES) if (!new File(root, "usr/lib64/" + name).isFile()) return false;
            if (!new File(root, "usr/bin/gamescopereaper").canExecute()) return false;
            return Files.isSameFile(new File(root, "usr/lib64/libstdc++.so.6").toPath(), new File(root, "usr/lib64/libstdc++.so.6.0.35").toPath());
        } catch (IOException e) { return false; }
    }

    private static boolean allowed(String path) {
        return path.equals("usr/bin/gamescopereaper") || path.startsWith("usr/lib64/") || path.startsWith("usr/share/licenses/") ||
                path.startsWith("usr/share/libinput/") || path.startsWith("usr/share/libwacom/");
    }

    private static void clear(File staging) throws IOException {
        if (!Files.exists(staging.toPath(), LinkOption.NOFOLLOW_LINKS)) return;
        try (Stream<Path> paths = Files.walk(staging.toPath())) {
            for (Path path : (Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator) Files.delete(path);
        }
    }

    public static synchronized void install(InputStream source, File root, File staging, String hash) throws IOException {
        if (ready(root, hash)) return;
        try {
            clear(staging);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[65536];
            int n;
            while ((n = source.read(buffer)) != -1) {
                if (out.size() + n > 16 * 1024 * 1024) throw new IOException("Gaming payload too large");
                out.write(buffer, 0, n);
            }
            byte[] data = out.toByteArray();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder actual = new StringBuilder();
            for (byte value : digest.digest(data)) actual.append(String.format(Locale.ROOT, "%02x", value & 255));
            if (!actual.toString().equals(hash)) throw new IOException("Gaming payload checksum mismatch");
            RootfsArchive.extract(new ByteArrayInputStream(data), staging, 64L * 1024 * 1024);
            List<Path> entries = new ArrayList<>();
            try (Stream<Path> paths = Files.walk(staging.toPath())) {
                paths.filter(path -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)).forEach(entries::add);
            }
            for (Path entry : entries) {
                String name = staging.toPath().relativize(entry).toString();
                if (!allowed(name) || ArchivePaths.inside(root, name) == null) throw new IOException("Invalid gaming payload path");
                if (Files.isSymbolicLink(entry)) {
                    Path link = Files.readSymbolicLink(entry);
                    if (link.isAbsolute() || !entry.getParent().resolve(link).normalize().startsWith(staging.toPath()))
                        throw new IOException("Invalid gaming library link");
                }
            }
            for (String library : LIBRARIES) if (!new File(staging, "usr/lib64/" + library).isFile()) throw new IOException("Gaming library missing: " + library);
            if (!new File(staging, "usr/bin/gamescopereaper").canExecute()) throw new IOException("Gaming reaper missing or not executable");
            for (Path entry : entries) {
                File target = new File(root, staging.toPath().relativize(entry).toString());
                Files.createDirectories(target.getParentFile().toPath());
                Files.move(entry, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            File marker = new File(root, MARKER + ".tmp");
            Files.write(marker.toPath(), hash.getBytes("UTF-8"));
            Files.move(marker.toPath(), new File(root, MARKER).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        } finally { clear(staging); }
    }
}
