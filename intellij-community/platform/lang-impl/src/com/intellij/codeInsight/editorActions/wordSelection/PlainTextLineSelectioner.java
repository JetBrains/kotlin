/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.codeInsight.editorActions.wordSelection;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPlainText;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlainTextLineSelectioner extends ExtendWordSelectionHandlerBase {
  @Override
  public boolean canSelect(@NotNull PsiElement e) {
    return e instanceof PsiPlainText;
  }

  @Override
  public List<TextRange> select(@NotNull PsiElement e, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
    return selectPlainTextLine(e, editorText, cursorOffset);
  }

  public static List<TextRange> selectPlainTextLine(final PsiElement e, final CharSequence editorText, final int cursorOffset) {
    int start = cursorOffset;
    while (start > 0 && editorText.charAt(start - 1) != '\n' && editorText.charAt(start - 1) != '\r') start--;

    int end = cursorOffset;
    while (end < editorText.length() && editorText.charAt(end) != '\n' && editorText.charAt(end) != '\r') end++;

    final TextRange range = new TextRange(start, end);
    if (!e.getParent().getTextRange().contains(range)) return null;
    List<TextRange> result = new ArrayList<>();
    result.add(range);
    return result;
  }
}
