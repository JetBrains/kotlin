// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util;

import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class DeleteNameDescriptionLocation extends ElementDescriptionLocation {
  private DeleteNameDescriptionLocation() {
  }

  public static final DeleteNameDescriptionLocation INSTANCE = new DeleteNameDescriptionLocation();
  private static final ElementDescriptionProvider ourDefaultProvider = new DefaultProvider();

  @NotNull
  @Override
  public ElementDescriptionProvider getDefaultProvider() {
    return ourDefaultProvider;
  }

  private static class DefaultProvider implements ElementDescriptionProvider {
    @Override
    public String getElementDescription(@NotNull final PsiElement element, @NotNull final ElementDescriptionLocation location) {
      if (location instanceof DeleteNameDescriptionLocation) {
        if (element instanceof PsiNamedElement) {
          return ((PsiNamedElement)element).getName();
        }
      }
      return null;
    }
  }
}
