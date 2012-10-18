package org.jetbrains.js.compiler;

import java.io.File;

public interface SourceMapBuilder {
    void newLine();

    void addMapping(String source, int sourceLine, int sourceColumn);

    void processSourceInfo(Object info);

    void addLink();

    File getOutFile();

    String build();
}
