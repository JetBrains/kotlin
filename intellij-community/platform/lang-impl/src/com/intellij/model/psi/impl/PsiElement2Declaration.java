// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl;

import com.intellij.model.Symbol;
import com.intellij.model.psi.PsiSymbolDeclaration;
import com.intellij.model.psi.PsiSymbolService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PsiDeclaredTarget;
import com.intellij.pom.references.PomService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class PsiElement2Declaration implements PsiSymbolDeclaration {

  private static final Logger LOG = Logger.getInstance(PsiElement2Declaration.class);

  private final PsiElement myTargetElement;
  private final PsiElement myDeclaringElement;
  private final TextRange myDeclarationRange;

  private PsiElement2Declaration(@NotNull PsiElement targetElement, @NotNull PsiElement declaringElement, @NotNull TextRange range) {
    myTargetElement = targetElement;
    myDeclaringElement = declaringElement;
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

  @Nullable
  static PsiSymbolDeclaration createFromPsi(@NotNull PsiElement targetElement) {
    if (targetElement instanceof PsiNameIdentifierOwner) {
      PsiElement identifyingElement = ((PsiNameIdentifierOwner)targetElement).getIdentifyingElement();
      if (identifyingElement != null) {
        return new PsiElement2Declaration(targetElement, identifyingElement, rangeOf(identifyingElement));
      }
    }
    return null;
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
        return identifyingElement.getTextRange().shiftLeft(declaringElement.getTextRange().getStartOffset());
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
      PsiDeclaredTarget declaredTarget = (PsiDeclaredTarget)target;
      TextRange nameIdentifierRange = declaredTarget.getNameIdentifierRange();
      if (nameIdentifierRange != null) {
        PsiElement navigationElement = declaredTarget.getNavigationElement();
        if (navigationElement == declaringElement) {
          return nameIdentifierRange;
        }
        else {
          LOG.assertTrue(navigationElement.getContainingFile() == declaringElement.getContainingFile());
          int delta = declaringElement.getTextRange().getStartOffset() - navigationElement.getTextRange().getStartOffset();
          return nameIdentifierRange.shiftLeft(delta);
        }
      }
    }
    return rangeOf(declaringElement);
  }

  @NotNull
  private static TextRange rangeOf(@NotNull PsiElement declaringElement) {
    return TextRange.from(0, declaringElement.getTextLength());
  }
}
