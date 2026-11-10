package com.learnloop.util;

import jakarta.servlet.http.Part;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialFileStorageTest {

    @TempDir
    Path directory;

    @Test
    void storesAllowedFileWithGeneratedNameOutsideWebRoot() throws IOException {
        MaterialFileStorage storage = new MaterialFileStorage(directory);
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        Part part = new TestPart("Week 1 notes.pdf", "application/pdf", content);

        MaterialFileStorage.StoredFile stored = storage.store(part);

        assertTrue(stored.storedName().endsWith(".pdf"));
        assertNotEquals("Week 1 notes.pdf", stored.storedName());
        assertEquals("Week 1 notes.pdf", stored.originalName());
        assertEquals("application/pdf", stored.mimeType());
        assertEquals(content.length, stored.sizeBytes());
        assertEquals("hello", Files.readString(storage.resolve(stored.storedName())));

        storage.deleteQuietly(stored.storedName());
        assertFalse(Files.exists(storage.resolve(stored.storedName())));
    }

    @Test
    void ignoresClientMimeTypeAndDerivesStoredTypeFromExtension() throws IOException {
        MaterialFileStorage storage = new MaterialFileStorage(directory);
        MaterialFileStorage.StoredFile stored = storage.store(
                new TestPart("fake.pdf", "text/html", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)));

        assertEquals("application/pdf", stored.mimeType());
        storage.deleteQuietly(stored.storedName());
    }

    @Test
    void mapsResponseMimeTypesFromKnownExtensionsAndUsesBinaryFallbackOtherwise() {
        assertEquals("application/pdf", MaterialFileStorage.mimeTypeForExtension("notes.PDF"));
        assertEquals("image/jpeg", MaterialFileStorage.mimeTypeForExtension("photo.jpeg"));
        assertEquals("text/plain; charset=UTF-8", MaterialFileStorage.mimeTypeForExtension("notes.txt"));
        assertEquals("application/octet-stream", MaterialFileStorage.mimeTypeForExtension("untrusted.html"));
        assertEquals("application/octet-stream", MaterialFileStorage.mimeTypeForExtension("no-extension"));
    }

    @Test
    void rejectsBlockedAndUnknownExtensions() {
        MaterialFileStorage storage = new MaterialFileStorage(directory);
        for (String name : List.of("page.html", "image.svg", "script.js", "malware.exe",
                "shell.sh", "run.bat", "index.php", "no-extension")) {
            assertThrows(IllegalArgumentException.class,
                    () -> storage.store(new TestPart(name, "text/plain", new byte[]{1})),
                    name);
        }
    }

    @Test
    void rejectsEmptyAndOversizedFiles() {
        MaterialFileStorage storage = new MaterialFileStorage(directory);
        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new TestPart("empty.pdf", "application/pdf", new byte[0])));
        assertThrows(IllegalArgumentException.class,
                () -> storage.store(new TestPart("huge.pdf", "application/pdf",
                        new byte[(int) MaterialFileStorage.MAX_FILE_SIZE_BYTES + 1])));
    }

    @Test
    void sanitizesOriginalFileName() {
        assertEquals("notes.pdf", MaterialFileStorage.sanitizeFileName("../../notes.pdf"));
        assertEquals("my_file.pdf", MaterialFileStorage.sanitizeFileName("C:\\temp\\my:file.pdf"));
        assertEquals("name.pdf", MaterialFileStorage.sanitizeFileName(" name.pdf... "));
        assertEquals(null, MaterialFileStorage.sanitizeFileName("..."));
    }

    @Test
    void blocksStoredNameTraversal() {
        MaterialFileStorage storage = new MaterialFileStorage(directory);
        assertThrows(IllegalArgumentException.class, () -> storage.resolve("../secret.pdf"));
        assertThrows(IllegalArgumentException.class, () -> storage.resolve("nested/../../secret.pdf"));
        assertThrows(IllegalArgumentException.class, () -> storage.resolve(""));
    }

    @Test
    void reportsAllowedExtensions() {
        assertTrue(MaterialFileStorage.isAllowedExtension("notes.PDF"));
        assertTrue(MaterialFileStorage.isAllowedExtension("photo.jpeg"));
        assertFalse(MaterialFileStorage.isAllowedExtension("page.html"));
        assertFalse(MaterialFileStorage.isAllowedExtension(null));
    }

    private static final class TestPart implements Part {

        private final String submittedFileName;
        private final String contentType;
        private final byte[] content;

        private TestPart(String submittedFileName, String contentType, byte[] content) {
            this.submittedFileName = submittedFileName;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getSubmittedFileName() {
            return submittedFileName;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public void write(String fileName) throws IOException {
            Files.writeString(Path.of(fileName), new String(content, StandardCharsets.UTF_8));
        }

        @Override
        public void delete() {
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return List.of();
        }

        @Override
        public Collection<String> getHeaderNames() {
            return List.of();
        }
    }
}
