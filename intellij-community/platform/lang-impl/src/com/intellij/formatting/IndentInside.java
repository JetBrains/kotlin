/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.formatting;

import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IndentInside {
  public int whiteSpaces = 0;
  public int tabs = 0;

  public IndentInside() {
  }

  public IndentInside(int whiteSpaces, int tabs) {
    this.whiteSpaces = whiteSpaces;
    this.tabs = tabs;
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final IndentInside indent = (IndentInside)o;

    if (tabs != indent.tabs) return false;
    return whiteSpaces == indent.whiteSpaces;
  }

  public int hashCode() {
    int result;
    result = whiteSpaces;
    result = 29 * result + tabs;
    return result;
  }

  public int getTabsCount(final CommonCodeStyleSettings.IndentOptions options) {
    final int tabsFromSpaces = whiteSpaces / options.TAB_SIZE;
    return tabs + tabsFromSpaces;
  }

  public int getSpacesCount(final CommonCodeStyleSettings.IndentOptions options) {
    return whiteSpaces + tabs * options.TAB_SIZE;
  }

  @NotNull
  static IndentInside getLastLineIndent(@NotNull final CharSequence text) {
    CharSequence lastLine = getLastLine(text);
    return createIndentOn(lastLine);
  }

  @NotNull
  public static IndentInside createIndentOn(@Nullable final CharSequence lastLine) {
    final IndentInside result = new IndentInside();
    if (lastLine == null) {
      return result;
    }
    for (int i = 0; i < lastLine.length(); i++) {
      if (lastLine.charAt(i) == ' ') result.whiteSpaces += 1;
      if (lastLine.charAt(i) == '\t') result.tabs += 1;
    }
    return result;
  }

  @NotNull
  public static CharSequence getLastLine(@NotNull final CharSequence text) {
    int i = CharArrayUtil.shiftBackwardUntil(text, text.length() - 1, "\n");
    if (i < 0) {
      return text;
    }
    else if (i >= text.length() - 1) {
      return "";
    }
    else {
      return text.subSequence(i + 1, text.length());
    }
  }

  @Override
  public String toString() {
    return String.format("spaces: %d, tabs: %d", whiteSpaces, tabs);
  }
}
