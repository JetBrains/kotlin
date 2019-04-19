/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
public class DefaultNonCodeSearchElementDescriptionProvider implements ElementDescriptionProvider {
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.util.DefaultNonCodeSearchElementDescriptionProvider");

  public static final DefaultNonCodeSearchElementDescriptionProvider INSTANCE = new DefaultNonCodeSearchElementDescriptionProvider();

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
