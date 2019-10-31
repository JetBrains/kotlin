// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author AG
 * @author yole
 */
public class SelectionQuotingTypedHandler extends TypedHandlerDelegate {
  public static final ExtensionPointName<UnquotingFilter> EP_NAME = ExtensionPointName.create("com.intellij.selectionUnquotingFilter");

  @NotNull
  @Override
  public Result beforeSelectionRemoved(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    SelectionModel selectionModel = editor.getSelectionModel();
    if (CodeInsightSettings.getInstance().AUTOINSERT_PAIR_QUOTE && selectionModel.hasSelection() && isDelimiter(c)) {
      String selectedText = selectionModel.getSelectedText();
      if (selectedText != null && selectedText.length() == 1) {
        char selectedChar = selectedText.charAt(0);
        if (isSimilarDelimiters(selectedChar, c) &&
            selectedChar != c &&
            !shouldSkipReplacementOfQuotesOrBraces(file, editor, selectedText, c) &&
            replaceQuotesBySelected(c, editor, file, selectionModel, selectedChar)) {
          return Result.STOP;
        }
      }
    }
    if (CodeInsightSettings.getInstance().SURROUND_SELECTION_ON_QUOTE_TYPED && selectionModel.hasSelection() && isDelimiter(c)) {
      String selectedText = selectionModel.getSelectedText();
      if (!StringUtil.isEmpty(selectedText)) {
        final int selectionStart = selectionModel.getSelectionStart();
        final int selectionEnd = selectionModel.getSelectionEnd();
        if (selectedText.length() > 1) {
          final char firstChar = selectedText.charAt(0);
          final char lastChar = selectedText.charAt(selectedText.length() - 1);
          if (isSimilarDelimiters(firstChar, c) && lastChar == getMatchingDelimiter(firstChar) &&
              (isQuote(firstChar) || firstChar != c) && !shouldSkipReplacementOfQuotesOrBraces(file, editor, selectedText, c) &&
              !containsQuoteInside(selectedText, lastChar)) {
            selectedText = selectedText.substring(1, selectedText.length() - 1);
          }
        }
        final int caretOffset = selectionModel.getSelectionStart();
        final char c2 = getMatchingDelimiter(c);
        final String newText = c + selectedText + c2;
        boolean ltrSelection = selectionModel.getLeadSelectionOffset() != selectionModel.getSelectionEnd();
        boolean restoreStickySelection = editor instanceof EditorEx && ((EditorEx)editor).isStickySelection();
        selectionModel.removeSelection();
        editor.getDocument().replaceString(selectionStart, selectionEnd, newText);
        TextRange replacedTextRange = new TextRange(caretOffset + 1, caretOffset + newText.length() - 1);
        // selection is removed here
        if (replacedTextRange.getEndOffset() <= editor.getDocument().getTextLength()) {
          if (restoreStickySelection) {
            EditorEx editorEx = (EditorEx)editor;
            CaretModel caretModel = editorEx.getCaretModel();
            caretModel.moveToOffset(ltrSelection ? replacedTextRange.getStartOffset() : replacedTextRange.getEndOffset());
            editorEx.setStickySelection(true);
            caretModel.moveToOffset(ltrSelection ? replacedTextRange.getEndOffset() : replacedTextRange.getStartOffset());
          }
          else {
            if (ltrSelection || editor instanceof EditorWindow) {
              editor.getSelectionModel().setSelection(replacedTextRange.getStartOffset(), replacedTextRange.getEndOffset());
            }
            else {
              editor.getSelectionModel().setSelection(replacedTextRange.getEndOffset(), replacedTextRange.getStartOffset());
            }
            editor.getCaretModel().moveToOffset(ltrSelection ? replacedTextRange.getEndOffset() : replacedTextRange.getStartOffset());
          }
        }
        return Result.STOP;
      }
    }
    return super.beforeSelectionRemoved(c, project, editor, file);
  }

  private static boolean containsQuoteInside(String selectedText, char quote) {
    return selectedText.indexOf(quote, 1) != selectedText.length() - 1;
  }

  private static boolean replaceQuotesBySelected(char c,
                                                 @NotNull Editor editor,
                                                 @NotNull PsiFile file,
                                                 @NotNull SelectionModel selectionModel,
                                                 char selectedChar) {
    int selectionStart = selectionModel.getSelectionStart();
    PsiElement element = file.findElementAt(selectionStart);
    while (element != null) {
      TextRange textRange = element.getTextRange();
      if (textRange != null && textRange.getLength() >= 2) {
        boolean isAtStart = textRange.getStartOffset() == selectionStart;
        if (isAtStart || textRange.getEndOffset() == selectionStart + 1 && isQuote(c)) {
          int matchingCharOffset = isAtStart ? textRange.getEndOffset() - 1 : textRange.getStartOffset();
          Document document = editor.getDocument();
          CharSequence charsSequence = document.getCharsSequence();
          if (matchingCharOffset < charsSequence.length()) {
            char matchingChar = charsSequence.charAt(matchingCharOffset);
            boolean otherQuoteMatchesSelected =
              isAtStart ? matchingChar == getMatchingDelimiter(selectedChar) : selectedChar == getMatchingDelimiter(matchingChar);
            if (otherQuoteMatchesSelected &&
                !containsQuoteInside(document.getText(textRange), charsSequence.charAt(textRange.getEndOffset() - 1))) {
              replaceChar(document, textRange.getStartOffset(), c);
              char c2 = getMatchingDelimiter(c);
              replaceChar(document, textRange.getEndOffset() - 1, c2);
              editor.getCaretModel().moveToOffset(selectionModel.getSelectionEnd());
              selectionModel.removeSelection();
              return true;
            }
          }
        }
      }
      if (element instanceof PsiFile) break;
      element = element.getParent();
    }
    return false;
  }

  private static boolean shouldSkipReplacementOfQuotesOrBraces(PsiFile psiFile, Editor editor, String selectedText, char c) {
    return EP_NAME.getExtensionList().stream().anyMatch(filter -> filter.skipReplacementQuotesOrBraces(psiFile, editor, selectedText, c));
  }

  private static char getMatchingDelimiter(char c) {
    if (c == '(') return ')';
    if (c == '[') return ']';
    if (c == '{') return '}';
    if (c == '<') return '>';
    return c;
  }

  private static boolean isDelimiter(final char c) {
    return isBracket(c) || isQuote(c);
  }

  private static boolean isBracket(final char c) {
    return c == '(' || c == '{' || c == '[' || c == '<';
  }

  private static boolean isQuote(final char c) {
    return c == '"' || c == '\'' || c == '`';
  }

  private static boolean isSimilarDelimiters(final char c1, final char c2) {
    return (isBracket(c1) && isBracket(c2)) || (isQuote(c1) && isQuote(c2));
  }

  private static void replaceChar(Document document, int offset, char newChar) {
    document.replaceString(offset, offset + 1, String.valueOf(newChar));
  }

  public static abstract class UnquotingFilter {
    public abstract boolean skipReplacementQuotesOrBraces(@NotNull PsiFile file,
                                                          @NotNull Editor editor,
                                                          @NotNull String selectedText,
                                                          char c);
  }
}
