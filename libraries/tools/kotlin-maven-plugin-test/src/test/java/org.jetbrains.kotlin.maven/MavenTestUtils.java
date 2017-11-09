package org.jetbrains.kotlin.maven;

import kotlin.io.FilesKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;

import java.io.*;

class MavenTestUtils {
    @NotNull
    static String readText(@NotNull File file) throws IOException {
        return FilesKt.readText(file, Charsets.UTF_8);
    }

    static void writeText(@NotNull File file, @NotNull String text) throws IOException {
        FilesKt.writeText(file, text, Charsets.UTF_8);
    }

    static void replaceFirstInFile(@NotNull File file, @NotNull String regex, @NotNull String replacement) throws IOException {
        String text = readText(file);
        String processedText = text.replaceFirst(regex, replacement);
        writeText(file, processedText);
    }

    @NotNull
    static String getNotNullSystemProperty(@NotNull String propertyName) {
        String value = System.getProperty(propertyName);
        if (value == null) {
            throw new IllegalStateException("A system property '" + propertyName + "' is not set");
        }
        return value;
    }
}
