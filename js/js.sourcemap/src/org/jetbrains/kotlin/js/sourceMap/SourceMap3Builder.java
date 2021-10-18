/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.sourceMap;

import com.intellij.openapi.util.text.StringUtil;
import gnu.trove.TObjectIntHashMap;
import kotlin.io.TextStreamsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.parser.sourcemaps.*;
import org.jetbrains.kotlin.js.util.TextOutput;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SourceMap3Builder implements SourceMapBuilder {
    private final StringBuilder out = new StringBuilder(8192);
    private final File generatedFile;
    private final TextOutput textOutput;
    private final String pathPrefix;

    private final TObjectIntHashMap<SourceKey> sources = new TObjectIntHashMap<SourceKey>() {
        @Override
        public int get(SourceKey key) {
            int index = index(key);
            return index < 0 ? -1 : _values[index];
        }
    };

    private final List<String> orderedSources = new ArrayList<>();
    private final List<Supplier<Reader>> orderedSourceContentSuppliers = new ArrayList<>();

    private int previousGeneratedColumn = -1;
    private int previousSourceIndex;
    private int previousSourceLine;
    private int previousSourceColumn;
    private int previousMappingOffset;
    private int previousPreviousSourceIndex;
    private int previousPreviousSourceLine;
    private int previousPreviousSourceColumn;
    private boolean currentMappingIsEmpty = true;

    public SourceMap3Builder(File generatedFile, TextOutput textOutput, String pathPrefix) {
        this.generatedFile = generatedFile;
        this.textOutput = textOutput;
        this.pathPrefix = pathPrefix;
    }

    @Override
    public File getOutFile() {
        return new File(generatedFile.getParentFile(), generatedFile.getName() + ".map");
    }

    @Override
    public String build() {
        @SuppressWarnings("unchecked")
        JsonObject json = new JsonObject();
        json.getProperties().put("version", new JsonNumber(3));
        if (generatedFile != null) json.getProperties().put("file", new JsonString(generatedFile.getName()));
        appendSources(json);
        appendSourcesContent(json);
        json.getProperties().put("names", new JsonArray());
        json.getProperties().put("mappings", new JsonString(out.toString()));
        return json.toString();
    }

    private void appendSources(JsonObject json) {
        JsonArray array = new JsonArray();
        for (String source : orderedSources) {
            array.getElements().add(new JsonString(pathPrefix + source));
        }
        json.getProperties().put("sources", array);
    }

    private void appendSourcesContent(JsonObject json) {
        JsonArray array = new JsonArray();
        for (Supplier<Reader> contentSupplier : orderedSourceContentSuppliers) {
            try (Reader reader = contentSupplier.get()) {
                array.getElements().add(reader != null ? new JsonString(TextStreamsKt.readText(reader)) : JsonNull.INSTANCE);
            }
            catch (IOException e) {
                //noinspection UseOfSystemOutOrSystemErr
                System.err.println("An exception occured during embedding sources into source map");
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
                // can't close the content reader or read from it
            }
        }
        json.getProperties().put("sourcesContent", array);
    }

    @Override
    public void newLine() {
        out.append(';');
        previousGeneratedColumn = -1;
    }

    @Override
    public void skipLinesAtBeginning(int count) {
        out.insert(0, StringUtil.repeatSymbol(';', count));
    }

    private int getSourceIndex(String source, Object identityObject, Supplier<Reader> contentSupplier) {
        SourceKey key = new SourceKey(source, identityObject);
        int sourceIndex = sources.get(key);
        if (sourceIndex == -1) {
            sourceIndex = orderedSources.size();
            sources.put(key, sourceIndex);
            orderedSources.add(source);
            orderedSourceContentSuppliers.add(contentSupplier);
        }

        return sourceIndex;
    }

    @Override
    public void addMapping(
            @NotNull String source, @Nullable Object identityObject, @NotNull Supplier<Reader> sourceContent,
            int sourceLine, int sourceColumn
    ) {
        source = source.replace(File.separatorChar, '/');
        int sourceIndex = getSourceIndex(source, identityObject, sourceContent);

        if (!currentMappingIsEmpty && previousSourceIndex == sourceIndex && previousSourceLine == sourceLine &&
            previousSourceColumn == sourceColumn) {
            return;
        }

        startMapping();

        Base64VLQ.encode(out, sourceIndex - previousSourceIndex);
        previousSourceIndex = sourceIndex;

        Base64VLQ.encode(out, sourceLine - previousSourceLine);
        previousSourceLine = sourceLine;

        Base64VLQ.encode(out, sourceColumn - previousSourceColumn);
        previousSourceColumn = sourceColumn;

        currentMappingIsEmpty = false;
    }

    @Override
    public void addEmptyMapping() {
        if (!currentMappingIsEmpty) {
            startMapping();
            currentMappingIsEmpty = true;
        }
    }

    private void startMapping() {
        boolean newGroupStarted = previousGeneratedColumn == -1;
        if (newGroupStarted) {
            previousGeneratedColumn = 0;
        }

        int columnDiff = textOutput.getColumn() - previousGeneratedColumn;
        if (!newGroupStarted) {
            out.append(',');
        }

        if (columnDiff > 0 || newGroupStarted) {
            Base64VLQ.encode(out, columnDiff);
            previousGeneratedColumn = textOutput.getColumn();

            previousMappingOffset = out.length();
            previousPreviousSourceIndex = previousSourceIndex;
            previousPreviousSourceLine = previousSourceLine;
            previousPreviousSourceColumn = previousSourceColumn;
        }
        else {
            out.setLength(previousMappingOffset);
            previousSourceIndex = previousPreviousSourceIndex;
            previousSourceLine = previousPreviousSourceLine;
            previousSourceColumn = previousPreviousSourceColumn;
        }
    }

    @Override
    public void addLink() {
        textOutput.print("\n//# sourceMappingURL=");
        textOutput.print(generatedFile.getName());
        textOutput.print(".map\n");
    }

    private static final class Base64VLQ {
        // A Base64 VLQ digit can represent 5 bits, so it is base-32.
        private static final int VLQ_BASE_SHIFT = 5;
        private static final int VLQ_BASE = 1 << VLQ_BASE_SHIFT;

        // A mask of bits for a VLQ digit (11111), 31 decimal.
        private static final int VLQ_BASE_MASK = VLQ_BASE - 1;

        // The continuation bit is the 6th bit.
        private static final int VLQ_CONTINUATION_BIT = VLQ_BASE;

        @SuppressWarnings("SpellCheckingInspection")
        private static final char[] BASE64_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

        private Base64VLQ() {
        }

        private static int toVLQSigned(int value) {
            return value < 0 ? ((-value) << 1) + 1 : value << 1;
        }

        public static void encode(StringBuilder out, int value) {
            value = toVLQSigned(value);
            do {
                int digit = value & VLQ_BASE_MASK;
                value >>>= VLQ_BASE_SHIFT;
                if (value > 0) {
                    digit |= VLQ_CONTINUATION_BIT;
                }
                out.append(BASE64_MAP[digit]);
            }
            while (value > 0);
        }
    }

    static final class SourceKey {
        private final String sourcePath;
        private final Object identityKey;

        SourceKey(String sourcePath, Object identityKey) {
            this.sourcePath = sourcePath;
            this.identityKey = identityKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SourceKey)) return false;

            SourceKey key = (SourceKey) o;

            if (!sourcePath.equals(key.sourcePath)) return false;
            if (identityKey != null ? !identityKey.equals(key.identityKey) : key.identityKey != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = sourcePath.hashCode();
            result = 31 * result + (identityKey != null ? identityKey.hashCode() : 0);
            return result;
        }
    }
}
