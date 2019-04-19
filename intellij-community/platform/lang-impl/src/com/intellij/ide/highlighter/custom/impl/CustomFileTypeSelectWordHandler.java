// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.highlighter.custom.impl;

import com.intellij.codeInsight.editorActions.BraceMatcherBasedSelectioner;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author yole
 */
public class CustomFileTypeSelectWordHandler extends BraceMatcherBasedSelectioner {
  @Override
  public boolean canSelect(@NotNull PsiElement e) {
    return e.getContainingFile().getFileType() instanceof CustomSyntaxTableFileType;
  }

  @Override
  public List<TextRange> select(@NotNull PsiElement e, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
    List<TextRange> superResult = super.select(e, editorText, cursorOffset, editor);

    HighlighterIterator iterator = ((EditorEx)editor).getHighlighter().createIterator(cursorOffset);
    if (CustomFileTypeQuoteHandler.isQuotedToken(iterator.getTokenType())) {
      List<TextRange> result = ContainerUtil.newArrayList();
      int start = iterator.getStart();
      int end = iterator.getEnd();
      char limitingQuote = CustomFileTypeQuoteHandler.getLimitingQuote(iterator.getTokenType());
      int startAfterQuote = start + ((start < editorText.length() && editorText.charAt(start) == limitingQuote) ? 1 : 0);
      int endBeforeQuote = end - ((end > 0 && editorText.charAt(end - 1) == limitingQuote) ? 1 : 0);
      if (start < startAfterQuote && end > endBeforeQuote && (end - start) > 1) result.add(new TextRange(start, end));
      if (startAfterQuote < endBeforeQuote) result.add(new TextRange(startAfterQuote, endBeforeQuote));
      if (superResult != null) {
        result.addAll(superResult);
      }
      return result;
    }
    
    return superResult;
  }
}
