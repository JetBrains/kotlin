// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.filters;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public final class FilterPositionUtil {
  @Nullable
  public static PsiElement searchNonSpaceNonCommentBack(PsiElement element) {
    return searchNonSpaceNonCommentBack(element, false);
  }

  @Nullable
  public static PsiElement searchNonSpaceNonCommentBack(PsiElement element, final boolean strict) {
    if (element == null || element.getNode() == null) return null;
    ASTNode leftNeighbour = TreeUtil.prevLeaf(element.getNode());
    if (!strict) {
      while (leftNeighbour != null &&
             (leftNeighbour.getElementType() == TokenType.WHITE_SPACE ||
              PsiTreeUtil.getNonStrictParentOfType(leftNeighbour.getPsi(), PsiComment.class) != null)) {
        leftNeighbour = TreeUtil.prevLeaf(leftNeighbour);
      }
    }
    return leftNeighbour != null ? leftNeighbour.getPsi() : null;
  }
}