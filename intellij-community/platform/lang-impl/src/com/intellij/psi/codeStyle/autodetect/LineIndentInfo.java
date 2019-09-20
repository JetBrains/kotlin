/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.psi.codeStyle.autodetect;

import org.jetbrains.annotations.NotNull;

public class LineIndentInfo {
  public static final LineIndentInfo EMPTY_LINE = new LineIndentInfo(LineType.EMPTY_LINE, -1);
  public static final LineIndentInfo LINE_WITH_COMMENT = new LineIndentInfo(LineType.LINE_WITH_COMMENT, -1);
  public static final LineIndentInfo LINE_WITH_TABS = new LineIndentInfo(LineType.LINE_WITH_TABS, -1);
  public static final LineIndentInfo LINE_WITH_CONTINUATION_INDENT = new LineIndentInfo(LineType.LINE_WITH_CONTINUATION_INDENT, -1);
  public static final LineIndentInfo LINE_WITH_NOT_COUNTABLE_INDENT = new LineIndentInfo(LineType.LINE_WITH_NOT_COUNTABLE_INDENT, -1);

  private final int myIndentSize;
  private final LineType myType;

  private LineIndentInfo(@NotNull LineType type, int indentSize) {
    myType = type;
    myIndentSize = indentSize;
  }

  @NotNull
  public static LineIndentInfo newNormalIndent(int indentSize) {
    return new LineIndentInfo(LineType.LINE_WITH_NORMAL_WHITESPACE_INDENT, indentSize);
  }

  public int getIndentSize() {
    return myIndentSize;
  }

  public boolean isLineWithNormalIndent() {
    return myType == LineType.LINE_WITH_NORMAL_WHITESPACE_INDENT;
  }

  public boolean isLineWithTabs() {
    return myType == LineType.LINE_WITH_TABS;
  }

  private enum LineType {
    EMPTY_LINE,
    LINE_WITH_COMMENT,
    LINE_WITH_TABS,
    LINE_WITH_NORMAL_WHITESPACE_INDENT,
    LINE_WITH_CONTINUATION_INDENT,
    LINE_WITH_NOT_COUNTABLE_INDENT
  }
}
