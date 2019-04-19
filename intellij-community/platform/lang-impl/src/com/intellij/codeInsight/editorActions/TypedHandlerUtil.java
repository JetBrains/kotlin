// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class TypedHandlerUtil {
  private static int computeBracesBalance(@NotNull Editor editor,
                                         int offset,
                                         @NotNull IElementType lt,
                                         @NotNull IElementType gt,
                                         @NotNull TokenSet invalidInsideReference,
                                         boolean forwardDirection) {
    HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset);
    if (!forwardDirection && iterator.getTokenType() != gt) return -1;
    while ((forwardDirection && iterator.getStart() > 0
            || !forwardDirection && !iterator.atEnd())
           && !invalidInsideReference.contains(iterator.getTokenType())) {
      incLookup(!forwardDirection /*we're rewinding*/, iterator);
    }

    if ((forwardDirection || !iterator.atEnd()) &&
      invalidInsideReference.contains(iterator.getTokenType())) {
      incLookup(forwardDirection, iterator);
    }

    int balance = 0;
    while (!iterator.atEnd() && balance >= 0) {
      final IElementType tokenType = iterator.getTokenType();
      if (tokenType == lt) {
        if (forwardDirection) balance++; else balance--;
      }
      else if (tokenType == gt) {
        if (forwardDirection) balance--; else balance++;
      }
      else if (invalidInsideReference.contains(tokenType)) {
        break;
      }

      incLookup(forwardDirection, iterator);
    }
    return balance;
  }

  private static void incLookup(boolean forwardDirection, @NotNull HighlighterIterator iterator) {
    if (forwardDirection) {
      iterator.advance();
    }
    else {
      iterator.retreat();
    }
  }

  public static boolean isAfterClassLikeIdentifierOrDot(final int offset,
                                                        @NotNull final Editor editor,
                                                        @NotNull final IElementType dot,
                                                        @NotNull final IElementType identifier,
                                                        boolean allowAfterDot) {
    HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset);
    if (iterator.atEnd()) return false;
    if (offset != iterator.getEnd() && iterator.getStart() > 0) iterator.retreat();
    final IElementType tokenType = iterator.getTokenType();
    if (allowAfterDot && tokenType == dot) return true;
    return isClassLikeIdentifier(offset, editor, iterator, identifier);
  }

  public static boolean isClassLikeIdentifier(final int offset,
                                              @NotNull final Editor editor,
                                              @NotNull final HighlighterIterator iterator,
                                              @NotNull final IElementType idType) {
    if (iterator.getTokenType() == idType && iterator.getEnd() == offset) {
      final CharSequence chars = editor.getDocument().getCharsSequence();
      final char startChar = chars.charAt(iterator.getStart());
      if (!Character.isUpperCase(startChar)) return false;
      final CharSequence word = chars.subSequence(iterator.getStart(), iterator.getEnd());
      if (word.length() == 1) return true;
      for (int i = 1; i < word.length(); i++) {
        if (Character.isLowerCase(word.charAt(i))) return true;
      }
    }

    return false;
  }

  public static void handleAfterGenericLT(@NotNull final Editor editor,
                                          @NotNull final IElementType lt,
                                          @NotNull final IElementType gt,
                                          @NotNull final TokenSet invalidInsideReference) {
    //need custom handler, since brace matcher cannot be used
    if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return;
    int offset = editor.getCaretModel().getOffset();
    if (computeBracesBalance(editor, offset, lt, gt, invalidInsideReference, true) == 1) {
      editor.getDocument().insertString(offset, ">");
      TabOutScopesTracker.getInstance().registerEmptyScope(editor, offset);
    }
  }

  public static boolean handleGenericGT(@NotNull final Editor editor,
                                        @NotNull final IElementType lt,
                                        @NotNull final IElementType gt,
                                        @NotNull final TokenSet invalidInsideReference) {
    //need custom handler, since brace matcher cannot be used
    if (!CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) return false;

    int offset = editor.getCaretModel().getOffset();
    if (offset == editor.getDocument().getTextLength()) return false;

    if (computeBracesBalance(editor, offset, lt, gt, invalidInsideReference, false) != 0) {
      return false;
    }

    EditorModificationUtil.moveCaretRelatively(editor, 1);
    return true;
  }

  public static void handleGenericLTDeletion(@NotNull final Editor editor,
                                             final int offset,
                                             @NotNull final IElementType lt,
                                             @NotNull final IElementType gt,
                                             @NotNull final TokenSet invalidInsideReference) {
    if (computeBracesBalance(editor, offset, lt, gt, invalidInsideReference, true) < 0) {
      editor.getDocument().deleteString(offset, offset + 1);
    }
  }
}
