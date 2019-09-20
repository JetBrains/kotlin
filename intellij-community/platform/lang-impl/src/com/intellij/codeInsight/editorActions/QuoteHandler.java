// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.lang.LanguageQuoteHandling;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;

/**
 * Implement this interface for {@link QuoteHandlerEP} to provide "Insert pair quote" functionality
 * for your language (controlled by {@link CodeInsightSettings#AUTOINSERT_PAIR_QUOTE}).
 * <p>
 * All the checks are triggered on typing/removal of a single quote and out of the box the aforementioned
 * functionality is supported only for ordinary strings enclosed in a single pair of quotes.
 * In order to support string literals that start and end with multiple quotes see {@link MultiCharQuoteHandler}
 * and {@link BackspaceHandlerDelegate}.
 *
 * @see CodeInsightSettings#AUTOINSERT_PAIR_QUOTE
 * @see QuoteHandlerEP
 * @see LanguageQuoteHandling
 * @see MultiCharQuoteHandler
 */
public interface QuoteHandler {
  /**
   * Checks whether there is the closing quote or the last one of the set of closing quotes
   * of a string literal at the given offset.
   * <p>
   * Upon insertion, this method is called <em>before</em> the quote is actually inserted in the editor
   * in order to check whether it's going to be typed over an existing one closing a string
   * literal and, thus, should be omitted (only the caret position will be adjusted in this case).
   *
   * @param iterator highlighting iterator at the state corresponding to the {@code offset}
   * @param offset   the offset at which the quote is going to be inserted/deleted
   */
  boolean isClosingQuote(HighlighterIterator iterator, int offset);

  /**
   * Checks whether there is the opening quote or the last one of the set of opening quotes
   * of a string literal at the given offset.
   *
   * @param iterator highlighting iterator at the state corresponding to the {@code offset}
   * @param offset   the offset at which the quote was inserted
   */
  boolean isOpeningQuote(HighlighterIterator iterator, int offset);

  /**
   * Called <em>after</em> the quote has been inserted in the editor to check that it belongs
   * to an empty string literal that has only the opening quote and therefore, can be completed
   * by inserting the respective closing one.
   * <p>
   * If both this method and {@link #isOpeningQuote(HighlighterIterator, int)}
   * return true, the respective closing quote will be inserted automatically.
   *
   * @param editor   an editor instance where the quote was inserted
   * @param iterator highlighting iterator at the state corresponding to the {@code offset}
   * @param offset   the offset at which the quote was inserted
   * @see #isOpeningQuote(HighlighterIterator, int)
   */
  boolean hasNonClosedLiteral(Editor editor, HighlighterIterator iterator, int offset);

  boolean isInsideLiteral(HighlighterIterator iterator);
}