// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Max Medvedev
 */
public interface MultiCharQuoteHandler extends QuoteHandler {
  /**
   * returns closing quote by opening quote which is placed immediately before offset. If there is no quote the method should return null
   */
  @Nullable
  CharSequence getClosingQuote(@NotNull HighlighterIterator iterator, int offset);

  /**
   * Should insert <code>closingQuote</code> returned from {@link #getClosingQuote(HighlighterIterator, int)} in the document. 
   */
  default void insertClosingQuote(@NotNull Editor editor, int offset, PsiFile file, @NotNull CharSequence closingQuote) {
    insertClosingQuote(editor, offset, closingQuote);
  }

  /**
   * Should insert <code>closingQuote</code> returned from {@link #getClosingQuote(HighlighterIterator, int)} in the document.
   * API compatibility method
   */
  default void insertClosingQuote(@NotNull Editor editor, int offset, @NotNull CharSequence closingQuote) {
    editor.getDocument().insertString(offset, closingQuote);
    if (closingQuote.length() == 1) {
      TabOutScopesTracker.getInstance().registerEmptyScope(editor, offset);
    }
  }
}
