/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.psi.codeStyle;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.SmartStripTrailingSpacesFilter;
import com.intellij.openapi.editor.StripTrailingSpacesFilter;
import com.intellij.openapi.editor.StripTrailingSpacesFilterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.editor.StripTrailingSpacesFilter.ALL_LINES;

public class KeepTrailingSpacesOnEmptyLinesFilterFactory extends StripTrailingSpacesFilterFactory {

  private static class KeepTrailingSpacesOnEmptyLinesFilter extends SmartStripTrailingSpacesFilter {
    private @NotNull final Document myDocument;

    KeepTrailingSpacesOnEmptyLinesFilter(@NotNull Document document) {
      myDocument = document;
    }

    @Override
    public int getTrailingSpacesToLeave(int line) {
      int startOffset = myDocument.getLineStartOffset(line);
      int endOffset = myDocument.getLineEndOffset(line);
      return containsWhitespacesOnly(myDocument.getCharsSequence(), startOffset, endOffset) ? getMaxIndentChars(line): 0;
    }


    private static boolean containsWhitespacesOnly(@NotNull CharSequence chars, int start, int end) {
      for (int i = start; i < end; i++) {
        final char c = chars.charAt(i);
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r') continue;
        return false;
      }
      return true;
    }
    
    private int getMaxIndentChars(int line) {
      int lineBefore = getNonEmptyLineBefore(line);
      int indentCharCount = -1;
      if (lineBefore >= 0) {
        indentCharCount = countIndentCharsAt(lineBefore);
      }
      int lineAfter = getNonEmptyLineAfter(line);
      if (lineAfter >= 0) {
        indentCharCount = Math.max(indentCharCount, countIndentCharsAt(lineAfter));
      }
      return indentCharCount;
    }
    
    private int getNonEmptyLineBefore(int line) {
      CharSequence docChars = myDocument.getCharsSequence();
      for (int lineBefore = line - 1; lineBefore >= 0; lineBefore --) {
        if (!containsWhitespacesOnly(docChars, myDocument.getLineStartOffset(lineBefore), myDocument.getLineEndOffset(lineBefore))) {
          return lineBefore;
        }
      }
      return -1;
    }
    
    private int getNonEmptyLineAfter(int line) {
      CharSequence docChars = myDocument.getCharsSequence();
      for (int lineAfter = line + 1; lineAfter < myDocument.getLineCount(); lineAfter ++) {
        if (!containsWhitespacesOnly(docChars, myDocument.getLineStartOffset(lineAfter), myDocument.getLineEndOffset(lineAfter))) {
          return lineAfter;
        }
      }
      return -1;
    }
    
    private int countIndentCharsAt(int line) {
      int count = 0;
      CharSequence docChars = myDocument.getCharsSequence();
      for (int offset = myDocument.getLineStartOffset(line); offset < myDocument.getTextLength(); offset ++) {
        char c = docChars.charAt(offset);
        if (c != ' ' && c != '\t') break;
        count ++;
      }
      return count;
    }
  }


  @NotNull
  @Override
  public StripTrailingSpacesFilter createFilter(@Nullable Project project, @NotNull Document document) {
    if (project != null && shouldKeepTrailingSpacesOnEmptyLines(project, document)) {
      return new KeepTrailingSpacesOnEmptyLinesFilter(document);
    }
    return ALL_LINES;
  }


  private static boolean shouldKeepTrailingSpacesOnEmptyLines(@NotNull Project project, @NotNull Document document) {
    PsiFile file = PsiDocumentManager.getInstance(project).getCachedPsiFile(document);
    if (file != null) {
      CommonCodeStyleSettings settings = CodeStyle.getLanguageSettings(file);
      CommonCodeStyleSettings.IndentOptions indentOptions = settings.getIndentOptions();
      return indentOptions != null && indentOptions.KEEP_INDENTS_ON_EMPTY_LINES;
    }
    return false;
  }
}
