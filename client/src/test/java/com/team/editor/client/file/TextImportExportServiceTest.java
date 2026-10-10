package com.team.editor.client.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TextImportService and TextExportService.
 */
class TextImportExportServiceTest {

    private final TextImportService importService = new TextImportService();
    private final TextExportService exportService = new TextExportService();

    @TempDir
    Path tempDir;

    // --- Import tests ---

    @Test
    void importText_readsFileContent() throws IOException {
        File file = tempDir.resolve("test.txt").toFile();
        Files.writeString(file.toPath(), "Hello World", StandardCharsets.UTF_8);

        String result = importService.importText(file);
        assertEquals("Hello World", result);
    }

    @Test
    void importText_preservesNewlines() throws IOException {
        File file = tempDir.resolve("multiline.txt").toFile();
        String content = "Line 1\nLine 2\nLine 3";
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);

        String result = importService.importText(file);
        assertEquals(content, result);
    }

    @Test
    void importText_preservesSpacesAndPunctuation() throws IOException {
        File file = tempDir.resolve("special.txt").toFile();
        String content = "  Hello,  World!  \n\tTabbed line.\n  Spaced out.  ";
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);

        String result = importService.importText(file);
        assertEquals(content, result);
    }

    @Test
    void importText_emptyFile() throws IOException {
        File file = tempDir.resolve("empty.txt").toFile();
        Files.writeString(file.toPath(), "", StandardCharsets.UTF_8);

        String result = importService.importText(file);
        assertEquals("", result);
    }

    @Test
    void importText_nullFileThrows() {
        assertThrows(IllegalArgumentException.class, () -> importService.importText(null));
    }

    @Test
    void importText_nonexistentFileThrows() {
        File file = tempDir.resolve("nonexistent.txt").toFile();
        assertThrows(IOException.class, () -> importService.importText(file));
    }

    // --- Export tests ---

    @Test
    void exportText_writesFileContent() throws IOException {
        File file = tempDir.resolve("export.txt").toFile();
        exportService.exportText(file, "Hello Export");

        String result = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        assertEquals("Hello Export", result);
    }

    @Test
    void exportText_preservesNewlines() throws IOException {
        File file = tempDir.resolve("export_multi.txt").toFile();
        String content = "Line 1\nLine 2\nLine 3";
        exportService.exportText(file, content);

        String result = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        assertEquals(content, result);
    }

    @Test
    void exportText_nullTextWritesEmpty() throws IOException {
        File file = tempDir.resolve("export_null.txt").toFile();
        exportService.exportText(file, null);

        String result = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        assertEquals("", result);
    }

    @Test
    void exportText_nullFileThrows() {
        assertThrows(IllegalArgumentException.class, () -> exportService.exportText(null, "text"));
    }

    // --- Round-trip test ---

    @Test
    void roundTrip_importExportPreservesContent() throws IOException {
        String original = "Hello\n  World  \n\tTabbed!\nSpecial chars: @#$%^&*()";

        File importFile = tempDir.resolve("roundtrip_in.txt").toFile();
        Files.writeString(importFile.toPath(), original, StandardCharsets.UTF_8);

        String imported = importService.importText(importFile);

        File exportFile = tempDir.resolve("roundtrip_out.txt").toFile();
        exportService.exportText(exportFile, imported);

        String exported = Files.readString(exportFile.toPath(), StandardCharsets.UTF_8);
        assertEquals(original, exported);
    }
}
