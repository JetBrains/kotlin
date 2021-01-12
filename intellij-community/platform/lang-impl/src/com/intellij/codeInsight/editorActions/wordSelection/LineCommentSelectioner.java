// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.editorActions.wordSelection;

import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.lang.Commenter;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LineCommentSelectioner extends WordSelectioner {
  @Override
  public boolean canSelect(@NotNull PsiElement e) {
    if (e instanceof PsiComment) {
      final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(e.getLanguage());
      if (!(commenter instanceof CodeDocumentationAwareCommenter)) return true;
      return !((CodeDocumentationAwareCommenter)commenter).isDocumentationComment((PsiComment)e);
    }
    return false;
  }

  @Override
  public List<TextRange> select(@NotNull PsiElement element, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
    List<TextRange> result = super.select(element, editorText, cursorOffset, editor);
    assert result != null;

    PsiElement firstComment = element;
    PsiElement e = element;
    while (e != null) {
      if (e instanceof PsiComment) {
        firstComment = e;
      }
      else if (!(e instanceof PsiWhiteSpace)) {
        break;
      }
      e = e.getPrevSibling();
    }

    PsiElement lastComment = element;
    e = element;
    while (e != null) {
      if (e instanceof PsiComment) {
        lastComment = e;
      }
      else if (!(e instanceof PsiWhiteSpace)) {
        break;
      }
      e = e.getNextSibling();
    }

    result.addAll(expandToWholeLine(editorText, new TextRange(firstComment.getTextRange().getStartOffset(),
                                                              lastComment.getTextRange().getEndOffset())));

    return result;
  }
}
