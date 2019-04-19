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

  public static DeleteNameDescriptionLocation INSTANCE = new DeleteNameDescriptionLocation();
  private static final ElementDescriptionProvider ourDefaultProvider = new DefaultProvider();

  @NotNull
  @Override
  public ElementDescriptionProvider getDefaultProvider() {
    return ourDefaultProvider;
  }

  public static class DefaultProvider implements ElementDescriptionProvider {
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
