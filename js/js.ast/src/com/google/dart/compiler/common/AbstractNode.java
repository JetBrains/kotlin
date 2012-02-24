// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.common;

import com.google.dart.compiler.Source;

/**
 * Abstract base class for nodes that carry source information.
 */
public class AbstractNode implements SourceInfo, HasSourceInfo {

  // TODO(johnlenz): All this source location data is wasteful.
  // Move it into a com.google.dart.compiler.backend.common object, that can be shared between the ASTs
  // or something.
  protected Source source = null;
  protected int sourceLine = -1;
  protected int sourceColumn = -1;
  protected int sourceStart = -1;
  protected int sourceLength = -1;

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public int getSourceLine() {
    return sourceLine;
  }

  @Override
  public int getSourceColumn() {
    return sourceColumn;
  }

  @Override
  public int getSourceStart() {
    return sourceStart;
  }

  @Override
  public int getSourceLength() {
    return sourceLength;
  }

  @Override
  public SourceInfo getSourceInfo() {
    return this;
  }

  @Override
  public void setSourceInfo(SourceInfo info) {
    source = info.getSource();
    sourceStart = info.getSourceStart();
    sourceLength = info.getSourceLength();
    sourceLine = info.getSourceLine();
    sourceColumn = info.getSourceColumn();
  }

  @Override
  public final void setSourceLocation(
      Source source, int line, int column, int startPosition, int length) {
    assert (startPosition != -1 && length >= 0
        || startPosition == -1 && length == 0);
    this.source = source;
    this.sourceLine = line;
    this.sourceColumn = column;
    this.sourceStart = startPosition;
    this.sourceLength = length;
  }

  public final void setSourceRange(int startPosition, int length) {
    assert (startPosition != -1 && length >= 0
        || startPosition == -1 && length == 0);
    this.sourceStart = startPosition;
    this.sourceLength = length;
  }

}
