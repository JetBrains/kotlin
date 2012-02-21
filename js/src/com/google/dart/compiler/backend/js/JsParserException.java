// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js;

/**
 * Indicates inability to parse JavaScript source.
 */
public class JsParserException extends Exception {

  /**
   * Represents the location of a parser exception.
   */
  public static class SourceDetail {
    private final String fileName;
    private final int line;
    private final int lineOffset;
    private final String lineSource;

    public SourceDetail(int line, String lineSource, int lineOffset, String fileName) {
      this.line = line;
      this.lineSource = lineSource;
      this.lineOffset = lineOffset;
      this.fileName = fileName;
    }

    public String getFileName() {
      return fileName;
    }

    public int getLine() {
      return line;
    }

    public int getLineOffset() {
      return lineOffset;
    }

    public String getLineSource() {
      return lineSource;
    }
  }

  private static String createMessageWithDetail(String msg, SourceDetail sourceDetail) {
    if (sourceDetail == null) {
      return msg;
    }
    StringBuffer sb = new StringBuffer();
    sb.append(sourceDetail.getFileName());
    sb.append('(');
    sb.append(sourceDetail.getLine());
    sb.append(')');
    sb.append(": ");
    sb.append(msg);
    if (sourceDetail.getLineSource() != null) {
      sb.append("\n> ");
      sb.append(sourceDetail.getLineSource());
      sb.append("\n> ");
      for (int i = 0, n = sourceDetail.getLineOffset(); i < n; ++i) {
        sb.append('-');
      }
      sb.append('^');
    }
    return sb.toString();
  }

  private final SourceDetail sourceDetail;

  public JsParserException(String msg) {
    this(msg, null);
  }

  public JsParserException(String msg, int line, String lineSource, int lineOffset, String fileName) {
    this(msg, new SourceDetail(line, lineSource, lineOffset, fileName));
  }

  public JsParserException(String msg, SourceDetail sourceDetail) {
    super(createMessageWithDetail(msg, sourceDetail));
    this.sourceDetail = sourceDetail;
  }

  /**
   * Provides additional source detail in some cases.
   * 
   * @return additional detail regarding the error, or <code>null</code> if no
   *         additional detail is available
   */
  public SourceDetail getSourceDetail() {
    return sourceDetail;
  }
}
