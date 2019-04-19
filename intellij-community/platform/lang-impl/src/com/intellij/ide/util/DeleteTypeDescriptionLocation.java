/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.ide.IdeBundle;
import com.intellij.lang.findUsages.LanguageFindUsages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.meta.PsiMetaOwner;
import com.intellij.psi.meta.PsiPresentableMetaData;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class DeleteTypeDescriptionLocation extends ElementDescriptionLocation {
  private final boolean myPlural;

  private DeleteTypeDescriptionLocation(final boolean plural) {
    myPlural = plural;
  }

  public static final DeleteTypeDescriptionLocation SINGULAR = new DeleteTypeDescriptionLocation(false);
  public static final DeleteTypeDescriptionLocation PLURAL = new DeleteTypeDescriptionLocation(true);

  private static final ElementDescriptionProvider ourDefaultProvider = new DefaultProvider();

  @NotNull
  @Override
  public ElementDescriptionProvider getDefaultProvider() {
    return ourDefaultProvider;
  }

  public boolean isPlural() {
    return myPlural;
  }

  public static class DefaultProvider implements ElementDescriptionProvider {
    @Override
    public String getElementDescription(@NotNull final PsiElement element, @NotNull final ElementDescriptionLocation location) {
      if (location instanceof DeleteTypeDescriptionLocation) {
        final boolean plural = ((DeleteTypeDescriptionLocation)location).isPlural();
        final int count = plural ? 2 : 1;
        if (element instanceof PsiFileSystemItem && PsiUtilBase.isSymLink((PsiFileSystemItem)element)) {
          return IdeBundle.message("prompt.delete.symlink", count);
        }
        if (element instanceof PsiFile) {
          return IdeBundle.message("prompt.delete.file", count);
        }
        if (element instanceof PsiDirectory) {
          return IdeBundle.message("prompt.delete.directory", count);
        }
        PsiMetaData metaData = element instanceof PsiMetaOwner ? ((PsiMetaOwner)element).getMetaData() : null;
        String typeName = metaData instanceof PsiPresentableMetaData ? ((PsiPresentableMetaData)metaData).getTypeName() : null;
        if (typeName == null) {
          typeName = LanguageFindUsages.getType(element);
        }
        return !plural ? typeName : StringUtil.pluralize(typeName);
      }
      return null;
    }
  }
}
