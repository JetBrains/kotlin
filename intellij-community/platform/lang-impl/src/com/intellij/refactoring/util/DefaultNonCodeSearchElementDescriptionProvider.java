// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.meta.PsiMetaOwner;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
class DefaultNonCodeSearchElementDescriptionProvider implements ElementDescriptionProvider {
  private static final Logger LOG = Logger.getInstance(DefaultNonCodeSearchElementDescriptionProvider.class);

  static final DefaultNonCodeSearchElementDescriptionProvider INSTANCE = new DefaultNonCodeSearchElementDescriptionProvider();

  @Override
  public String getElementDescription(@NotNull final PsiElement element, @NotNull final ElementDescriptionLocation location) {
    if (!(location instanceof NonCodeSearchDescriptionLocation)) return null;
    final NonCodeSearchDescriptionLocation ncdLocation = (NonCodeSearchDescriptionLocation)location;

    if (element instanceof PsiDirectory) {
      if (ncdLocation.isNonJava()) {
        final String qName = PsiDirectoryFactory.getInstance(element.getProject()).getQualifiedName((PsiDirectory)element, false);
        if (qName.length() > 0) return qName;
        return null;
      }
      return ((PsiDirectory) element).getName();
    }

    if (element instanceof PsiMetaOwner) {
      final PsiMetaOwner psiMetaOwner = (PsiMetaOwner)element;
      final PsiMetaData metaData = psiMetaOwner.getMetaData();
      if (metaData != null) {
        return metaData.getName();
      }
    }
    if (element instanceof PsiNamedElement) {
      return ((PsiNamedElement)element).getName();
    }
    else {
     // LOG.error("Unknown element type: " + element);
      return null;
    }
  }
}
