// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.navigation.PsiElementNavigationItem;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PsiElementsEqualityProvider extends AbstractEqualityProvider {

  @Override
  public boolean areEqual(@NotNull SearchEverywhereFoundElementInfo newItemInfo, @NotNull SearchEverywhereFoundElementInfo alreadyFoundItemInfo) {
    PsiElement newElementPsi = toPsi(newItemInfo.getElement());
    PsiElement alreadyFoundPsi = toPsi(alreadyFoundItemInfo.getElement());

    return newElementPsi != null && alreadyFoundPsi != null
           && Objects.equals(newElementPsi, alreadyFoundPsi);
  }

  @Nullable
  public static PsiElement toPsi(Object o) {
    if (o instanceof PsiElement) {
      return  (PsiElement)o;
    }

    if (o instanceof PsiElementNavigationItem) {
      return  ((PsiElementNavigationItem)o).getTargetElement();
    }

    return null;
  }
}
