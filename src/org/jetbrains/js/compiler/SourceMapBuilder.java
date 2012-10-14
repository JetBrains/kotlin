package org.jetbrains.js.compiler;

import com.google.dart.compiler.common.SourceInfo;
import com.google.dart.compiler.util.TextOutput;
import com.intellij.util.PairConsumer;
import gnu.trove.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.List;

public class SourceMapBuilder {
    private final StringBuilder out = new StringBuilder();
    private final String generatedFilename;
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

    private int previousGeneratedColumn;
    private int previousSourceIndex;
    private int previousSourceLine;
    private int previousSourceColumn;

    public SourceMapBuilder(String generatedFilename, TextOutput textOutput, PairConsumer<SourceMapBuilder, Object> sourceInfoConsumer) {
        this.generatedFilename = generatedFilename;
        this.textOutput = textOutput;
        this.sourceInfoConsumer = sourceInfoConsumer;
    }

    public String getOutFilename() {
        return generatedFilename + ".map";
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"version\":3,\"file\":\"").append(generatedFilename).append('"').append(',');
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
            sb.append('"').append(source).append('"');
        }
        sb.append(']');
    }

    public void newLine() {
        out.append(';');
        previousGeneratedColumn = 0;
    }

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
            sourceIndex = sources.put(source, orderedSources.size());
            orderedSources.add(source);
        }

        lastSource = source;
        lastSourceIndex = sourceIndex;

        return sourceIndex;
    }

    public void addMapping(String source, int sourceLine, int sourceColumn) {
        if (previousGeneratedColumn != 0) {
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

    private static int toVLQSigned(int value) {
        return value < 0 ? ((-value) << 1) + 1 : value << 1;
    }

    public void addLink() {
        textOutput.print("\n//@ sourceMappingURL=");
        textOutput.print(getOutFilename());
    }

    private static final class Base64VLQ {
        // A Base64 VLQ digit can represent 5 bits, so it is base-32.
        private static final int VLQ_BASE_SHIFT = 5;
        private static final int VLQ_BASE = 1 << VLQ_BASE_SHIFT;

        // A mask of bits for a VLQ digit (11111), 31 decimal.
        private static final int VLQ_BASE_MASK = VLQ_BASE - 1;

        // The continuation bit is the 6th bit.
        private static final int VLQ_CONTINUATION_BIT = VLQ_BASE;

        /**
         * A map used to convert integer values in the range 0-63 to their base64
         * values.
         */
        private static final String BASE64_MAP =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "abcdefghijklmnopqrstuvwxyz" +
                "0123456789+/";

        public static void encode(StringBuilder out, int value) {
            value = toVLQSigned(value);
            do {
                int digit = value & VLQ_BASE_MASK;
                value >>>= VLQ_BASE_SHIFT;
                if (value > 0) {
                    digit |= VLQ_CONTINUATION_BIT;
                }
                out.append(toBase64(digit));
            }
            while (value > 0);
        }

        public static char toBase64(int value) {
            assert (value <= 63 && value >= 0) : "value out of range:" + value;
            return BASE64_MAP.charAt(value);
        }
    }
}
