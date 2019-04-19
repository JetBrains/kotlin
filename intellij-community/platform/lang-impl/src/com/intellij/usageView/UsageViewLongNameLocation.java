/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package com.intellij.usageView;

import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class UsageViewLongNameLocation extends ElementDescriptionLocation {
  private UsageViewLongNameLocation() {
  }

  public static final UsageViewLongNameLocation INSTANCE = new UsageViewLongNameLocation();

  @NotNull
  @Override
  public ElementDescriptionProvider getDefaultProvider() {
    return DEFAULT_PROVIDER;
  }

  private static final ElementDescriptionProvider DEFAULT_PROVIDER = new ElementDescriptionProvider() {
    @Override
    public String getElementDescription(@NotNull final PsiElement element, @NotNull final ElementDescriptionLocation location) {
      if (location instanceof UsageViewLongNameLocation) {
        if (element instanceof PsiDirectory) {
          return PsiDirectoryFactory.getInstance(element.getProject()).getQualifiedName((PsiDirectory)element, true);
        }
        if (element instanceof PsiQualifiedNamedElement) {
          return ((PsiQualifiedNamedElement)element).getQualifiedName();
        }
        return UsageViewShortNameLocation.INSTANCE.getDefaultProvider().getElementDescription(
          element, UsageViewShortNameLocation.INSTANCE);
      }
      return null;
    }
  };
}
