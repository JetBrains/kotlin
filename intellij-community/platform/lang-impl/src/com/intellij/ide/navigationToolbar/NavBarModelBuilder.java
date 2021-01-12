// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Creates a new model for navigation bar by taking a giving element and
 *  traverse path to root adding each element to a model
 */
public abstract class NavBarModelBuilder {

  public static NavBarModelBuilder getInstance() {
    return ServiceManager.getService(NavBarModelBuilder.class);
  }

  public List<Object> createModel(@NotNull PsiElement psiElement,
                                  @NotNull Set<VirtualFile> roots,
                                  @Nullable NavBarModelExtension ownerExtension) {
    final List<Object> model = new ArrayList<>();
    traverseToRoot(psiElement, roots, model, ownerExtension);
    return model;
  }

  abstract void traverseToRoot(@NotNull PsiElement psiElement,
                               @NotNull Set<VirtualFile> roots,
                               @NotNull List<Object> model,
                               @Nullable NavBarModelExtension ownerExtension);
}

