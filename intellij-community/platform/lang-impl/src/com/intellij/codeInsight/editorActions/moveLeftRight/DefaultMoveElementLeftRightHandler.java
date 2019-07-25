// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions.moveLeftRight;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiListLikeElement;
import org.jetbrains.annotations.NotNull;

public class DefaultMoveElementLeftRightHandler extends MoveElementLeftRightHandler {

  @NotNull
  @Override
  public PsiElement[] getMovableSubElements(@NotNull PsiElement element) {
    if (element instanceof PsiListLikeElement) {
      return ((PsiListLikeElement)element).getComponents().toArray(PsiElement.EMPTY_ARRAY);
    }
    else {
      return PsiElement.EMPTY_ARRAY;
    }
  }
}
