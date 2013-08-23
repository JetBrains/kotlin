package org.jetbrains.js.compiler.sourcemap;

import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.util.TextOutput;
import com.intellij.util.PairConsumer;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.js.compiler.SourceMapBuilder;

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

    private final List<String> orderedSources = new ArrayList<String>();

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
        if (previousGeneratedColumn == -1) {
            previousGeneratedColumn = 0;
        }
        else {
            out.append(',');
        }

        Base64VLQ.encode(out, textOutput.getColumn() - previousGeneratedColumn);
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
        textOutput.print("\n//@ sourceMappingURL=file://");
        textOutput.print(getOutFile().getAbsolutePath());
    }

    private static final class Base64VLQ {
        // A Base64 VLQ digit can represent 5 bits, so it is base-32.
        private static final int VLQ_BASE_SHIFT = 5;
        private static final int VLQ_BASE = 1 << VLQ_BASE_SHIFT;

        // A mask of bits for a VLQ digit (11111), 31 decimal.
        private static final int VLQ_BASE_MASK = VLQ_BASE - 1;

        // The continuation bit is the 6th bit.
        private static final int VLQ_CONTINUATION_BIT = VLQ_BASE;

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
