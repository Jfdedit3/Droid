package com.droiddeck.launcher.core;

import org.apache.commons.compress.archivers.tar.*;
import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;
import static org.junit.Assert.*;

public class RootfsArchiveTest {
    private byte[] archive(String name, byte type, String link, String content) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GZIPOutputStream(out))) {
            TarArchiveEntry e = new TarArchiveEntry(name, type, true);
            e.setMode(0755);
            if (link != null) e.setLinkName(link);
            byte[] data = content == null ? new byte[0] : content.getBytes("UTF-8");
            e.setSize(data.length);
            tar.putArchiveEntry(e);
            tar.write(data);
            tar.closeArchiveEntry();
        }
        return out.toByteArray();
    }

    private File root() throws Exception { return Files.createTempDirectory("debian-rootfs-").toFile(); }

    @Test public void extractsFilesWithExecutablePermission() throws Exception {
        File root = root();
        RootfsArchive.extract(new ByteArrayInputStream(archive("usr/bin/program", (byte)'0', null, "hello")), root, 1024);
        File file = new File(root, "usr/bin/program");
        assertEquals("hello", new String(Files.readAllBytes(file.toPath()), "UTF-8"));
        assertTrue(file.canExecute());
    }

    @Test public void preservesUsrMergeSymlinks() throws Exception {
        File root = root();
        RootfsArchive.extract(new ByteArrayInputStream(archive("bin", (byte)'2', "usr/bin", null)), root, 1024);
        assertEquals("usr/bin", Files.readSymbolicLink(new File(root, "bin").toPath()).toString());
    }

    @Test public void rejectsTraversal() throws Exception {
        assertThrows(IOException.class, () -> RootfsArchive.extract(new ByteArrayInputStream(archive("../escape", (byte)'0', null, "x")), root(), 1024));
    }

    @Test public void rejectsAbsolutePaths() throws Exception {
        assertThrows(IOException.class, () -> RootfsArchive.extract(new ByteArrayInputStream(archive("/escape", (byte)'0', null, "x")), root(), 1024));
    }

    @Test public void rejectsWritesThroughEscapingSymlinks() throws Exception {
        File root = root();
        File outside = Files.createTempDirectory("debian-outside-").toFile();
        Files.createSymbolicLink(new File(root, "link").toPath(), outside.toPath());
        assertThrows(IOException.class, () -> RootfsArchive.extract(new ByteArrayInputStream(archive("link/escape", (byte)'0', null, "x")), root, 1024));
        assertFalse(new File(outside, "escape").exists());
    }

    @Test public void rejectsHardLinks() throws Exception {
        assertThrows(IOException.class, () -> RootfsArchive.extract(new ByteArrayInputStream(archive("hard", (byte)'1', "target", null)), root(), 1024));
    }

    @Test public void enforcesExpandedSizeLimit() throws Exception {
        assertThrows(IOException.class, () -> RootfsArchive.extract(new ByteArrayInputStream(archive("large", (byte)'0', null, "12345")), root(), 4));
    }
}
