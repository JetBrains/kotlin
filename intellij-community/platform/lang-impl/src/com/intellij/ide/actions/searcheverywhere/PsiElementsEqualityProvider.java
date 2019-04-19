// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.navigation.PsiElementNavigationItem;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PsiElementsEqualityProvider implements SEResultsEqualityProvider {

  @NotNull
  @Override
  public SEEqualElementsActionType compareItems(@NotNull SearchEverywhereFoundElementInfo newItemInfo, @NotNull SearchEverywhereFoundElementInfo alreadyFoundItemInfo) {
    PsiElement newElementPsi = toPsi(newItemInfo.getElement());
    PsiElement alreadyFoundPsi = toPsi(alreadyFoundItemInfo.getElement());

    if (newElementPsi == null || alreadyFoundPsi == null) {
      return SEEqualElementsActionType.DO_NOTHING;
    }

    if (Objects.equals(newElementPsi, alreadyFoundPsi)) {
      return newItemInfo.priority > alreadyFoundItemInfo.priority ? SEEqualElementsActionType.REPLACE : SEEqualElementsActionType.SKIP;
    }

    return SEEqualElementsActionType.DO_NOTHING;
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
