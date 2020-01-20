// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.findUsages;

import com.intellij.lang.findUsages.LanguageFindUsages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
*/
public final class DefaultFindUsagesHandlerFactory extends FindUsagesHandlerFactory {

  @Internal
  public static final class DefaultFindUsagesHandler extends FindUsagesHandler {
    DefaultFindUsagesHandler(@NotNull PsiElement psiElement) {
      super(psiElement);
    }
  }

  @Override
  public boolean canFindUsages(@NotNull final PsiElement element) {
    if (!element.isValid()) {
      return false;
    }
    if (element instanceof PsiFileSystemItem) {
      return ((PsiFileSystemItem)element).getVirtualFile() != null;
    }
    return LanguageFindUsages.canFindUsagesFor(element);
  }

  @Override
  public FindUsagesHandler createFindUsagesHandler(@NotNull final PsiElement element, final boolean forHighlightUsages) {
    return new DefaultFindUsagesHandler(element);
  }
}
