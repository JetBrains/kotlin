// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.common;

import com.google.common.base.Preconditions;
import com.google.dart.compiler.Source;

public class AbstractNode implements SourceInfo, HasSourceInfo {
    protected SourceInfo sourceInfo;

    @Override
    public Source getSource() {
        return sourceInfo.getSource();
    }

    @Override
    public int getLine() {
        return sourceInfo.getLine();
    }

    @Override
    public int getColumn() {
        return sourceInfo.getColumn();
    }

    @Override
    public int getStart() {
        return sourceInfo.getStart();
    }

    @Override
    public int getLength() {
        return sourceInfo.getLength();
    }

    @Override
    public SourceInfo getSourceInfo() {
        return this;
    }

    @Override
    public void setSourceInfo(SourceInfo info) {
        sourceInfo = info;
    }

    @Override
    public final void setSourceLocation(Source source, int line, int column, int startPosition, int length) {
        sourceInfo = new SourceInfoImpl(source, line, column, startPosition, length);
    }

    public final void setSourceRange(int startPosition, int length) {
        Preconditions.checkArgument(startPosition != -1 && length >= 0 || startPosition == -1 && length == 0);
        //sourceInfo.sourceStart = startPosition;
        //sourceInfo.sourceLength = length;
    }
}
