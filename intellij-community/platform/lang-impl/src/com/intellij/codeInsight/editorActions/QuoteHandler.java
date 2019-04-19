// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.lang.LanguageQuoteHandling;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;

/**
 * @see QuoteHandlerEP
 * @see LanguageQuoteHandling
 */
public interface QuoteHandler {
  boolean isClosingQuote(HighlighterIterator iterator, int offset);

  boolean isOpeningQuote(HighlighterIterator iterator, int offset);

  boolean hasNonClosedLiteral(Editor editor, HighlighterIterator iterator, int offset);

  boolean isInsideLiteral(HighlighterIterator iterator);
}