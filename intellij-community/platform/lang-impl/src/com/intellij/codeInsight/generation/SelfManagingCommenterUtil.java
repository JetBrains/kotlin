/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.codeInsight.generation;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ignatov
 */
public class SelfManagingCommenterUtil {
  @Nullable
  public static TextRange getBlockCommentRange(int selectionStart,
                                               int selectionEnd,
                                               @NotNull Document document,
                                               @NotNull String prefix,
                                               @NotNull String suffix) {
    CharSequence sequence = document.getCharsSequence();
    selectionStart = CharArrayUtil.shiftForward(sequence, selectionStart, " \t\n");
    selectionEnd = CharArrayUtil.shiftBackward(sequence, selectionEnd - 1, " \t\n") + 1;

    if (selectionEnd < selectionStart) {
      selectionEnd = selectionStart;
    }

    if (CharArrayUtil.regionMatches(sequence, selectionEnd - suffix.length(), suffix) &&
        CharArrayUtil.regionMatches(sequence, selectionStart, prefix)) {
      return new TextRange(selectionStart, selectionEnd);
    }
    return null;
  }

  @NotNull
  public static TextRange insertBlockComment(int startOffset,
                                             int endOffset,
                                             @NotNull Document document,
                                             @NotNull String prefix,
                                             @NotNull String suffix) {
    document.insertString(startOffset, prefix);
    document.insertString(endOffset + prefix.length(), suffix);
    return new TextRange(startOffset, endOffset + prefix.length() + suffix.length());
  }

  public static void uncommentBlockComment(int startOffset,
                                           int endOffset,
                                           @NotNull Document document,
                                           @NotNull String prefix,
                                           @NotNull String suffix) {
    document.deleteString(endOffset - suffix.length(), endOffset);
    document.deleteString(startOffset, startOffset + prefix.length());
  }
}
