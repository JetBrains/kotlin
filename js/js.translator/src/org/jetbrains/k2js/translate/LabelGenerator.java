package org.jetbrains.k2js.translate;

public final class LabelGenerator {
    private int nameCounter;
    private final char prefix;

    public LabelGenerator(char prefix) {
        this.prefix = prefix;
    }

    public String generate() {
        return prefix + Integer.toString(nameCounter++, 36);
    }
}
