package com.google.dart.compiler.common;

import com.google.common.base.Preconditions;
import com.google.dart.compiler.Source;

public class SourceInfoImpl implements SourceInfo {
    protected Source source = null;
    protected int line = -1;
    protected int column = -1;
    protected int start = -1;
    protected int length = -1;

    public SourceInfoImpl(Source source, int line, int column, int start, int length) {
        Preconditions.checkArgument(start != -1 && length >= 0 || start == -1 && length == 0);

        this.source = source;
        this.line = line;
        this.column = column;
        this.start = start;
        this.length = length;
    }

    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getLength() {
        return length;
    }
}
