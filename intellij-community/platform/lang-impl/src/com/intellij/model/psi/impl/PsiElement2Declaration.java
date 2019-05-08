// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl;

import com.intellij.model.Symbol;
import com.intellij.model.psi.PsiSymbolDeclaration;
import com.intellij.model.psi.PsiSymbolService;
import com.intellij.openapi.util.TextRange;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PsiDeclaredTarget;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;

class PsiElement2Declaration implements PsiSymbolDeclaration {

  private final PsiElement myTargetElement;
  private final PsiElement myDeclaringElement;
  private final TextRange myDeclarationRange;

  private PsiElement2Declaration(@NotNull PsiElement targetElement, @NotNull PsiElement declaringElement, TextRange range) {
    myDeclaringElement = declaringElement;
    myTargetElement = targetElement;
    myDeclarationRange = range;
  }

  @NotNull
  @Override
  public Symbol getSymbol() {
    return PsiSymbolService.getInstance().asSymbol(myTargetElement);
  }

  @NotNull
  @Override
  public PsiElement getDeclaringElement() {
    return myDeclaringElement;
  }

  @NotNull
  @Override
  public TextRange getDeclarationRange() {
    return myDeclarationRange;
  }

  @NotNull
  static PsiSymbolDeclaration createFromPsi(@NotNull PsiElement targetElement, @NotNull PsiElement declaringElement) {
    return new PsiElement2Declaration(targetElement, declaringElement, getDeclarationRangeFromPsi(declaringElement));
  }

  @NotNull
  private static TextRange getDeclarationRangeFromPsi(@NotNull PsiElement declaringElement) {
    if (declaringElement instanceof PsiNameIdentifierOwner) {
      PsiElement identifyingElement = ((PsiNameIdentifierOwner)declaringElement).getIdentifyingElement();
      if (identifyingElement != null) {
        return identifyingElement.getTextRangeInParent();
      }
    }
    return rangeOf(declaringElement);
  }

  @NotNull
  static PsiSymbolDeclaration createFromPom(@NotNull PomTarget target, @NotNull PsiElement declaringElement) {
    return new PsiElement2Declaration(
      PomService.convertToPsi(declaringElement.getProject(), target),
      declaringElement,
      getDeclarationRangeFromPom(target, declaringElement)
    );
  }

  @NotNull
  private static TextRange getDeclarationRangeFromPom(@NotNull PomTarget target, @NotNull PsiElement declaringElement) {
    if (target instanceof PsiDeclaredTarget) {
      assert ((PsiDeclaredTarget)target).getNavigationElement() == declaringElement;
      TextRange nameIdentifierRange = ((PsiDeclaredTarget)target).getNameIdentifierRange();
      if (nameIdentifierRange != null) {
        return nameIdentifierRange;
      }
    }
    return rangeOf(declaringElement);
  }

  @NotNull
  private static TextRange rangeOf(@NotNull PsiElement declaringElement) {
    return TextRange.from(0, declaringElement.getTextLength());
  }
}
