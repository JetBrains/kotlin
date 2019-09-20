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

package com.intellij.codeInsight;

import com.intellij.application.options.CodeStyle;
import com.intellij.formatting.IndentData;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.util.codeInsight.CommentUtilCore;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;

public class CommentUtil extends CommentUtilCore {
  private CommentUtil() { }

  public static IndentData getMinLineIndent(Document document, int line1, int line2, @NotNull PsiFile file) {
    CharSequence chars = document.getCharsSequence();
    IndentData minIndent = null;
    for (int line = line1; line <= line2; line++) {
      int lineStart = document.getLineStartOffset(line);
      int textStart = CharArrayUtil.shiftForward(chars, lineStart, " \t");
      if (textStart >= document.getTextLength()) {
        textStart = document.getTextLength();
      }
      else {
        char c = chars.charAt(textStart);
        if (c == '\n' || c == '\r') continue; // empty line
      }
      IndentData indent = IndentData.createFrom(chars, lineStart, textStart, CodeStyle.getIndentOptions(file).TAB_SIZE);
      minIndent = IndentData.min(minIndent, indent);
    }
    if (minIndent == null && line1 == line2 && line1 < document.getLineCount() - 1) {
      return getMinLineIndent(document, line1 + 1, line1 + 1, file);
    }
    return minIndent;
  }
}
