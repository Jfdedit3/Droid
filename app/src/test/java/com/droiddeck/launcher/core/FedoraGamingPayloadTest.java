package com.droiddeck.launcher.core;

import org.apache.commons.compress.archivers.tar.*;
import org.junit.Test;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import static org.junit.Assert.*;

public class FedoraGamingPayloadTest {
    private static final String[] LIBS = {"libwayland-server.so.0", "libseat.so.1", "libinput.so.10", "libavif.so.16", "libeis.so.1", "libluajit-5.1.so.2", "libstdc++.so.6.0.35"};
    private byte[] archive(String extra, String link, boolean complete) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GZIPOutputStream(bytes))) {
            if (complete) for (String name : LIBS) {
                TarArchiveEntry entry = new TarArchiveEntry("usr/lib64/" + name);
                byte[] content = name.getBytes("UTF-8"); entry.setSize(content.length); entry.setMode(0755);
                tar.putArchiveEntry(entry); tar.write(content); tar.closeArchiveEntry();
            }
            if (complete) {
                TarArchiveEntry helper = new TarArchiveEntry("usr/bin/gamescopereaper"); helper.setMode(0755);
                tar.putArchiveEntry(helper);tar.closeArchiveEntry();
            }
            TarArchiveEntry alias = new TarArchiveEntry("usr/lib64/libstdc++.so.6", (byte)'2');
            alias.setLinkName("libstdc++.so.6.0.35");tar.putArchiveEntry(alias);tar.closeArchiveEntry();
            if (extra != null) {
                TarArchiveEntry entry = new TarArchiveEntry(extra, link == null ? (byte)'0' : (byte)'2');
                if (link != null) entry.setLinkName(link);
                tar.putArchiveEntry(entry);tar.closeArchiveEntry();
            }
        }
        return bytes.toByteArray();
    }
    private String hash(byte[] data) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(data)) result.append(String.format("%02x", value & 255));
        return result.toString();
    }
    private File temp() throws Exception { return Files.createTempDirectory("fedora-gaming-test-").toFile(); }
    private void install(byte[] data, File root, String hash) throws Exception {
        FedoraGamingPayload.install(new ByteArrayInputStream(data), root, new File(root.getParent(), root.getName()+"-staging"), hash);
    }
    @Test public void upgradesLibrariesWithoutChangingHomeOrOldRuntime() throws Exception {
        File root=temp(); File home=new File(root,"root/personal.txt");home.getParentFile().mkdirs();Files.write(home.toPath(),new byte[]{42});
        File old=new File(root,"usr/lib64/libstdc++.so.6.0.34");old.getParentFile().mkdirs();Files.write(old.toPath(),new byte[]{7});
        byte[] data=archive(null,null,true); install(data,root,hash(data));
        assertTrue(FedoraGamingPayload.ready(root,hash(data)));assertArrayEquals(new byte[]{42},Files.readAllBytes(home.toPath()));assertArrayEquals(new byte[]{7},Files.readAllBytes(old.toPath()));
    }
    @Test public void checksumFailureDoesNotModifyRoot() throws Exception {
        File root=temp();byte[] data=archive(null,null,true);
        assertThrows(IOException.class,()->install(data,root,"wrong"));assertFalse(new File(root,"usr").exists());
    }
    @Test public void refusesPersonalFilePathsBeforeCopyingAnyLibrary() throws Exception {
        File root=temp();byte[] data=archive("root/personal.txt",null,true);
        assertThrows(IOException.class,()->install(data,root,hash(data)));assertFalse(new File(root,"usr").exists());
    }
    @Test public void refusesEscapingLinks() throws Exception {
        File root=temp();byte[] data=archive("usr/lib64/escape","/tmp/outside",true);
        assertThrows(IOException.class,()->install(data,root,hash(data)));assertFalse(new File(root,"usr").exists());
    }
    @Test public void incompletePayloadDoesNotCreateSuccessMarker() throws Exception {
        File root=temp();byte[] data=archive(null,null,false);
        assertThrows(IOException.class,()->install(data,root,hash(data)));assertFalse(FedoraGamingPayload.ready(root,hash(data)));
    }
    @Test public void detectsRemovedLibraryAndCanRepair() throws Exception {
        File root=temp();byte[] data=archive(null,null,true);install(data,root,hash(data));
        Files.delete(new File(root,"usr/lib64/libseat.so.1").toPath());assertFalse(FedoraGamingPayload.ready(root,hash(data)));
        install(data,root,hash(data));assertTrue(FedoraGamingPayload.ready(root,hash(data)));
    }
    @Test public void installedPayloadDoesNotReadArchiveAgain() throws Exception {
        File root=temp();byte[] data=archive(null,null,true);install(data,root,hash(data));
        InputStream unreadable=new InputStream() { public int read() throws IOException { throw new IOException("must not read"); } };
        FedoraGamingPayload.install(unreadable,root,new File(root.getParent(),root.getName()+"-staging"),hash(data));
        assertTrue(FedoraGamingPayload.ready(root,hash(data)));
    }
    @Test public void detectsMissingReaperAndRepairsIt() throws Exception {
        File root=temp();byte[] data=archive(null,null,true);install(data,root,hash(data));
        Files.delete(new File(root,"usr/bin/gamescopereaper").toPath());assertFalse(FedoraGamingPayload.ready(root,hash(data)));
        install(data,root,hash(data));assertTrue(new File(root,"usr/bin/gamescopereaper").canExecute());assertTrue(FedoraGamingPayload.ready(root,hash(data)));
    }
}
