/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.sourceMap;

import org.jetbrains.kotlin.js.common.SourceInfo;
import org.jetbrains.kotlin.js.util.TextOutput;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PairConsumer;
import gnu.trove.TObjectIntHashMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SourceMap3Builder implements SourceMapBuilder {
    private final StringBuilder out = new StringBuilder(8192);
    private final File generatedFile;
    private final TextOutput textOutput;
    private final PairConsumer<SourceMapBuilder, Object> sourceInfoConsumer;

    private String lastSource;
    private int lastSourceIndex;

    private final TObjectIntHashMap<String> sources = new TObjectIntHashMap<String>() {
        @Override
        public int get(String key) {
            int index = index(key);
            return index < 0 ? -1 : _values[index];
        }
    };

    private final List<String> orderedSources = new ArrayList<>();

    private int previousGeneratedColumn = -1;
    private int previousSourceIndex;
    private int previousSourceLine;
    private int previousSourceColumn;

    public SourceMap3Builder(File generatedFile, TextOutput textOutput, PairConsumer<SourceMapBuilder, Object> sourceInfoConsumer) {
        this.generatedFile = generatedFile;
        this.textOutput = textOutput;
        this.sourceInfoConsumer = sourceInfoConsumer;
    }

    @Override
    public File getOutFile() {
        return new File(generatedFile.getParentFile(), generatedFile.getName() + ".map");
    }

    @Override
    public String build() {
        StringBuilder sb = new StringBuilder(out.length() + (128 * orderedSources.size()));
        sb.append("{\"version\":3,\"file\":\"").append(generatedFile.getName()).append('"').append(',');
        appendSources(sb);
        sb.append(",\"names\":[");
        sb.append("],\"mappings\":\"");
        sb.append(out);
        sb.append("\"}");
        return sb.toString();
    }

    private void appendSources(StringBuilder sb) {
        boolean isNotFirst = false;
        sb.append('"').append("sources").append("\":[");
        for (String source : orderedSources) {
            if (isNotFirst) {
                sb.append(',');
            }
            else {
                isNotFirst = true;
            }
            sb.append('"').append("file://").append(source).append('"');
        }
        sb.append(']');
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

    @Override
    public void processSourceInfo(Object sourceInfo) {
        if (sourceInfo instanceof SourceInfo) {
            throw new UnsupportedOperationException("SourceInfo is not yet supported");
        }
        sourceInfoConsumer.consume(this, sourceInfo);
    }

    private int getSourceIndex(String source) {
        if (source.equals(lastSource)) {
            return lastSourceIndex;
        }

        int sourceIndex = sources.get(source);
        if (sourceIndex == -1) {
            sourceIndex = orderedSources.size();
            sources.put(source, sourceIndex);
            orderedSources.add(source);
        }

        lastSource = source;
        lastSourceIndex = sourceIndex;

        return sourceIndex;
    }

    @Override
    public void addMapping(String source, int sourceLine, int sourceColumn) {
        boolean newGroupStarted = previousGeneratedColumn == -1;
        if (newGroupStarted) {
            previousGeneratedColumn = 0;
        }

        int columnDiff = textOutput.getColumn() - previousGeneratedColumn;
        if (!newGroupStarted && columnDiff == 0) {
            return;
        }
        if (!newGroupStarted) {
            out.append(',');
        }

        // TODO fix sections overlapping
        // assert columnDiff != 0;
        Base64VLQ.encode(out, columnDiff);
        previousGeneratedColumn = textOutput.getColumn();
        int sourceIndex = getSourceIndex(source);
        Base64VLQ.encode(out, sourceIndex - previousSourceIndex);
        previousSourceIndex = sourceIndex;

        Base64VLQ.encode(out, sourceLine - previousSourceLine);
        previousSourceLine = sourceLine;

        Base64VLQ.encode(out, sourceColumn - previousSourceColumn);
        previousSourceColumn = sourceColumn;
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
}
