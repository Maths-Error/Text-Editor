package com.team.editor.client.file;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Reads a local text file and returns its content as a String.
 */
public class TextImportService {

    /**
     * Reads the given file as UTF-8 text.
     *
     * @param file the text file to import
     * @return the file content as a String (preserving newlines, spaces, punctuation)
     * @throws IOException if the file cannot be read
     */
    public String importText(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null.");
        }
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new IOException("Not a regular file: " + file.getAbsolutePath());
        }

        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }
}
