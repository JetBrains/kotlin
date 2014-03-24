/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js;

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

    public SourceDetail(int line, String lineSource, int lineOffset,
        String fileName) {
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

  private static String createMessageWithDetail(String msg,
      SourceDetail sourceDetail) {
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

  public JsParserException(String msg, int line, String lineSource,
      int lineOffset, String fileName) {
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
