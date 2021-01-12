// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

public interface JavaLikeQuoteHandler extends QuoteHandler {
  @NotNull
  @SuppressWarnings("SpellCheckingInspection")
  TokenSet getConcatenatableStringTokenTypes();

  default boolean canBeConcatenated(@NotNull PsiElement element) {
    return getConcatenatableStringTokenTypes().contains(PsiUtilCore.getElementType(element));
  }

  String getStringConcatenationOperatorRepresentation();

  TokenSet getStringTokenTypes();

  boolean isAppropriateElementTypeForLiteral(@NotNull IElementType tokenType);

  boolean needParenthesesAroundConcatenation(PsiElement element);

}