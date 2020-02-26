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

  /**
   * Adapts target element of unknown origin to a {@code PsiSymbolDeclaration}.
   * E.g. when searching for declarations of a {@link Psi2Symbol PsiElement symbol} we lose info about origin,
   * because the symbol could be obtained from reference or another declaration or any other old code.
   */
  @Nullable
  static PsiSymbolDeclaration createFromTargetPsiElement(@NotNull PsiElement targetElement) {
    if (targetElement instanceof PsiNameIdentifierOwner) {
      PsiElement identifyingElement = ((PsiNameIdentifierOwner)targetElement).getIdentifyingElement();
      if (identifyingElement != null) {
        return new PsiElement2Declaration(targetElement, identifyingElement, rangeOf(identifyingElement));
      }
    }
    return null;
  }

  /**
   * Adapts target element obtained from an element at caret to a {@code PsiSymbolDeclaration}.
   *
   * @param declaredElement  target element (symbol); used for target-based actions, e.g. Find Usages
   * @param declaringElement element at caret from which {@code declaredElement} was obtained; used to determine the declaration range
   */
  @NotNull
  static PsiSymbolDeclaration createFromDeclaredPsiElement(@NotNull PsiElement declaredElement, @NotNull PsiElement declaringElement) {
    return new PsiElement2Declaration(declaredElement, declaringElement, getDeclarationRangeFromPsi(declaringElement));
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

  @Nullable
  static PsiSymbolDeclaration createFromPom(@NotNull PomTarget target, @NotNull PsiElement declaringElement) {
    TextRange declarationRange = getDeclarationRangeFromPom(target, declaringElement);
    if (declarationRange == null) {
      return null;
    }
    return new PsiElement2Declaration(
      PomService.convertToPsi(declaringElement.getProject(), target),
      declaringElement,
      declarationRange
    );
  }

  @Nullable
  private static TextRange getDeclarationRangeFromPom(@NotNull PomTarget target, @NotNull PsiElement declaringElement) {
    if (target instanceof PsiDeclaredTarget) {
      PsiDeclaredTarget declaredTarget = (PsiDeclaredTarget)target;
      TextRange nameIdentifierRange = declaredTarget.getNameIdentifierRange();
      if (nameIdentifierRange != null) {
        PsiElement navigationElement = declaredTarget.getNavigationElement();
        if (navigationElement == declaringElement) {
          return nameIdentifierRange;
        }
        else if (navigationElement.getContainingFile() == declaringElement.getContainingFile()) {
          int delta = declaringElement.getTextRange().getStartOffset() - navigationElement.getTextRange().getStartOffset();
          return nameIdentifierRange.shiftLeft(delta);
        }
        else {
          LOG.error("Navigation element file differs from declaring element file;\n" +
                    "target: " + target + ";\n" +
                    "target class: " + target.getClass().getName() + ";\n" +
                    "navigation element file: " + navigationElement.getContainingFile() + ";\n" +
                    "declaring element file: " + declaringElement.getContainingFile());
          return null;
        }
      }
    }
    return rangeOf(declaringElement);
  }

  /**
   * @return range of {@code element} relative to itself
   */
  @NotNull
  private static TextRange rangeOf(@NotNull PsiElement element) {
    return TextRange.from(0, element.getTextLength());
  }
}
