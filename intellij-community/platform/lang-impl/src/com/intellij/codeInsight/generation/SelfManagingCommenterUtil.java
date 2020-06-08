// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.generation;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author ignatov
 */
public final class SelfManagingCommenterUtil {
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
