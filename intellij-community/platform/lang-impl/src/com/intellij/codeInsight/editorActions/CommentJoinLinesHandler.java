// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class CommentJoinLinesHandler implements JoinRawLinesHandlerDelegate {

  @Override
  public int tryJoinLines(@NotNull Document document, @NotNull PsiFile file, int start, int end) {
    return CANNOT_JOIN;
  }

  @Override
  public int tryJoinRawLines(@NotNull Document document, @NotNull PsiFile file, int start, int end) {
    PsiElement prevElement = file.findElementAt(start);
    if (prevElement instanceof PsiWhiteSpace) {
      prevElement = file.findElementAt(start - 1);
    }
    PsiComment prevComment = PsiTreeUtil.getNonStrictParentOfType(prevElement, PsiComment.class);
    PsiComment nextComment = PsiTreeUtil.getNonStrictParentOfType(file.findElementAt(end), PsiComment.class);
    if (prevComment == null || nextComment == null) return CANNOT_JOIN;
    boolean sameComment = prevComment == nextComment;
    boolean adjacentLineComments = false;
    CharSequence text = document.getText();
    if (text.charAt(end) == '*' && end < text.length() && text.charAt(end + 1) != '/') {
      end = StringUtil.skipWhitespaceForward(text, end + 1);
    }
    else if (!sameComment &&
             !(start >= 2 && text.charAt(start - 2) == '*' && text.charAt(start - 1) == '/') &&
             text.charAt(end) == '/' && end + 1 < text.length() && text.charAt(end + 1) == '/') {
      adjacentLineComments = true;
      end = StringUtil.skipWhitespaceForward(text, end + 2);
    }

    document.replaceString(start, end, adjacentLineComments || sameComment ? " " : "");
    return start;
  }
}
