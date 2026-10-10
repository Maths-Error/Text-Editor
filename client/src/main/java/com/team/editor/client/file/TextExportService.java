package com.team.editor.client.file;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Writes text content to a local file as UTF-8.
 */
public class TextExportService {

    /**
     * Writes the given text to the specified file as UTF-8.
     *
     * @param file the target file to write to
     * @param text the text content to export
     * @throws IOException if the file cannot be written
     */
    public void exportText(File file, String text) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null.");
        }
        if (text == null) {
            text = "";
        }

        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
    }
}
