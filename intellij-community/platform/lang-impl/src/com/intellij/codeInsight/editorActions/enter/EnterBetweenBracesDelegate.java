// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions.enter;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtension;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnterBetweenBracesDelegate {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.editorActions.enter.EnterBetweenBracesDelegate");
  static final LanguageExtension<EnterBetweenBracesDelegate> EP_NAME = new LanguageExtension<>("com.intellij.enterBetweenBracesDelegate");

  public boolean bracesAreInTheSameElement(@NotNull PsiFile file, @NotNull Editor editor, int lBraceOffset, int rBraceOffset) {
    PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument());
    if (file.findElementAt(lBraceOffset) == file.findElementAt(rBraceOffset)) {
      return true;
    }
    return false;
  }

  protected void formatAtOffset(@NotNull PsiFile file, @NotNull Editor editor, int offset, @Nullable Language language) {
    PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument());
    try {
      CodeStyleManager.getInstance(file.getProject()).adjustLineIndent(file, offset);
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }

  public boolean isInComment(@NotNull PsiFile file, @NotNull Editor editor,  int offset) {
    return PsiTreeUtil.getParentOfType(file.findElementAt(offset), PsiComment.class) != null;
  }

  /**
   * @param lBrace left brace offset
   * @param rBrace right brace offset
   * @return true, if braces are pair for handling
   */
  protected boolean isBracePair(char lBrace, char rBrace) {
    return (lBrace == '(' && rBrace == ')') || (lBrace == '{' && rBrace == '}');
  }
}
