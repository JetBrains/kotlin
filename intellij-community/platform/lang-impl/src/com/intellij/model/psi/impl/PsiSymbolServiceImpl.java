// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl;

import com.intellij.model.Symbol;
import com.intellij.model.psi.PsiSymbolReference;
import com.intellij.model.psi.PsiSymbolService;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class PsiSymbolServiceImpl implements PsiSymbolService {

  @Contract(pure = true)
  @NotNull
  @Override
  public Symbol asSymbol(@NotNull PsiElement element) {
    if (element instanceof Symbol) {
      return (Symbol)element;
    }
    else {
      // consider all PsiElements obtained from references (or other APIs) as Symbols,
      // because that's what usually was meant
      return new Psi2Symbol(element);
    }
  }

  @Contract(pure = true)
  @Nullable
  @Override
  public PsiElement extractElementFromSymbol(@NotNull Symbol symbol) {
    if (symbol instanceof PsiElement) {
      return (PsiElement)symbol;
    }
    else if (symbol instanceof Psi2Symbol) {
      return ((Psi2Symbol)symbol).getElement();
    }
    else {
      // If the Symbol is brand new (not based on some PsiElement, not related to LightElement or PomTarget),
      // then the client should implement proper Symbol-based APIs,
      // hence we consider brand new Symbol implementations as inapplicable for old APIs
      return null;
    }
  }

  @Override
  public @NotNull Iterable<? extends @NotNull PsiSymbolReference> getOwnReferences(@NotNull PsiElement element) {
    return Arrays.asList(element.getReferences());
  }
}
