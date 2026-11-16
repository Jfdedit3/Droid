package com.droiddeck.launcher.core;

import java.io.*;
import java.nio.file.Files;
import org.apache.commons.compress.archivers.ar.*;
import org.apache.commons.compress.archivers.tar.*;
import org.tukaani.xz.*;
import org.junit.*;
import static org.junit.Assert.*;

public class BravePayloadTest {
    private File directory;
    @Before public void prepare() throws Exception { directory=Files.createTempDirectory("brave-payload-").toFile(); }
    @After public void cleanup() throws Exception {
        try (java.util.stream.Stream<java.nio.file.Path> paths=Files.walk(directory.toPath())) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }
    private byte[] packageWith(String[] names) throws Exception {
        ByteArrayOutputStream compressed=new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar=new TarArchiveOutputStream(new XZOutputStream(compressed,new LZMA2Options(0)))) {
            for(String name:names) {
                byte[] bytes="payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                TarArchiveEntry entry=new TarArchiveEntry(name);
                entry.setSize(bytes.length);entry.setMode(name.endsWith("/brave")?0755:0644);
                tar.putArchiveEntry(entry);tar.write(bytes);tar.closeArchiveEntry();
            }
        }
        ByteArrayOutputStream result=new ByteArrayOutputStream();
        try(ArArchiveOutputStream ar=new ArArchiveOutputStream(result)) {
            byte[] bytes=compressed.toByteArray();
            ar.putArchiveEntry(new ArArchiveEntry("data.tar.xz",bytes.length));ar.write(bytes);ar.closeArchiveEntry();
        }
        return result.toByteArray();
    }
    @Test public void extractsOnlyBrowserFilesAndPreservesExecutableMode() throws Exception {
        byte[] bytes=packageWith(new String[]{"./opt/brave.com/brave/brave","./opt/brave.com/brave/resources.pak","./usr/bin/unrelated"});
        BravePayload.extract(new ByteArrayInputStream(bytes),directory);
        assertTrue(new File(directory,"brave").canExecute());
        assertTrue(new File(directory,"resources.pak").isFile());
        assertFalse(new File(directory,"usr").exists());
    }
    @Test public void refusesTraversal() throws Exception {
        byte[] bytes=packageWith(new String[]{"opt/brave.com/brave/../escape"});
        try {BravePayload.extract(new ByteArrayInputStream(bytes),directory);fail("Traversal accepted");}
        catch(IOException expected) {assertFalse(new File(directory.getParentFile(),"escape").exists());}
    }
    @Test public void refusesIncompletePayload() throws Exception {
        byte[] bytes=packageWith(new String[]{"opt/brave.com/brave/brave"});
        try {BravePayload.extract(new ByteArrayInputStream(bytes),directory);fail("Incomplete browser accepted");}
        catch(IOException expected) {assertTrue(expected.getMessage().contains("Incomplete"));}
    }
}
