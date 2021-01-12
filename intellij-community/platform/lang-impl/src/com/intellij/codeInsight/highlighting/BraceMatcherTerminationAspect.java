package com.intellij.codeInsight.highlighting;

import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * In some cases brace matching should be terminated to prevent search for a paired brace beyond some element types.
 * For example, in a sequence like '{)}' the closing brace '}' does not belong to the same structural level as the
 * opening '{' brace because of enclosing ')' parenthesis. Therefore we should not treat '}' as a pair brace for '{'
 * before ')'.
 * <p>
 * In the described case a class implementing {@code PairedBraceMatcher} may also implement this aspect to
 * perform its own specific checks.
 * </p>
 *
 * @author Rustam Vishnyakov
 */
public interface BraceMatcherTerminationAspect {
  /**
   * Checks if a search for matching brace should be stopped with negative result if an element having the given type
   * is encountered.
   *
   * @param forward   The forward search flag.
   * @param braceType The type of the brace for which an opposite brace is searched for.
   * @param iterator  The iterator at the current position to be checked.
   * @return  True if the search should be stopped or false if it should continue.
   */
  boolean shouldStopMatch(boolean forward, @NotNull IElementType braceType, @NotNull HighlighterIterator iterator);
}
