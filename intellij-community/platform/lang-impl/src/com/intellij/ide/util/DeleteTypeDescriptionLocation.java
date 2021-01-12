// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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

  private static class DefaultProvider implements ElementDescriptionProvider {
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
