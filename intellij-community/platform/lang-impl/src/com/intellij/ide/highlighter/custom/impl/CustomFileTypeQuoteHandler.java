// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.highlighter.custom.impl;

import com.intellij.codeInsight.editorActions.QuoteHandler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.CustomHighlighterTokenType;
import com.intellij.psi.tree.IElementType;

/**
 * @author Maxim.Mossienko
 */
public class CustomFileTypeQuoteHandler implements QuoteHandler {
  @Override
  public boolean isClosingQuote(HighlighterIterator iterator, int offset) {
    final IElementType tokenType = iterator.getTokenType();

    if (isQuotedToken(tokenType)){
      int start = iterator.getStart();
      int end = iterator.getEnd();
      return end - start >= 1 && offset == end - 1;
    }
    return false;
  }

  static boolean isQuotedToken(IElementType tokenType) {
    return tokenType == CustomHighlighterTokenType.STRING ||
        tokenType == CustomHighlighterTokenType.SINGLE_QUOTED_STRING ||
        tokenType == CustomHighlighterTokenType.CHARACTER;
  }

  static char getLimitingQuote(IElementType quotedToken) {
    if (quotedToken == CustomHighlighterTokenType.STRING) return '"';
    else if (quotedToken == CustomHighlighterTokenType.SINGLE_QUOTED_STRING ||
             quotedToken == CustomHighlighterTokenType.CHARACTER) {
      return '\'';
    }
    else return 0;
  }

  @Override
  public boolean isOpeningQuote(HighlighterIterator iterator, int offset) {
    if (isQuotedToken(iterator.getTokenType())){
      int start = iterator.getStart();
      return offset == start;
    }
    return false;
  }

  @Override
  public boolean hasNonClosedLiteral(Editor editor, HighlighterIterator iterator, int offset) {
    try {
      Document doc = editor.getDocument();
      CharSequence chars = doc.getCharsSequence();
      int lineEnd = doc.getLineEndOffset(doc.getLineNumber(offset));

      while (!iterator.atEnd() && iterator.getStart() < lineEnd) {
        if (isQuotedToken(iterator.getTokenType())) {

          if (iterator.getStart() >= iterator.getEnd() - 1 ||
              chars.charAt(iterator.getEnd() - 1) != '\"' && chars.charAt(iterator.getEnd() - 1) != '\'') {
            return true;
          }
        }
        iterator.advance();
      }
    } finally {
      while (!iterator.atEnd() && iterator.getStart() != offset) iterator.retreat();
    }

    return false;
  }

  @Override
  public boolean isInsideLiteral(HighlighterIterator iterator) {
    return isQuotedToken(iterator.getTokenType());
  }
}
