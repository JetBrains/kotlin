// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author gregsh
 */
public abstract class AbstractNavBarModelExtension implements NavBarModelExtension {
  @Nullable
  @Override
  public abstract String getPresentableText(Object object);

  @Nullable
  @Override
  public PsiElement adjustElement(@NotNull PsiElement psiElement) {
    return psiElement;
  }

  @Nullable
  @Override
  public PsiElement getParent(PsiElement psiElement) {
    return null;
  }

  @NotNull
  @Override
  public Collection<VirtualFile> additionalRoots(Project project) {
    return Collections.emptyList();
  }
}
