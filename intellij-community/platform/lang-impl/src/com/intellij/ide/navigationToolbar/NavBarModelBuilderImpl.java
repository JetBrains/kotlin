// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class NavBarModelBuilderImpl extends NavBarModelBuilder {

  @Override
  public void traverseToRoot(@NotNull PsiElement psiElement, @NotNull Set<VirtualFile> roots, @NotNull List<Object> model) {

    List<NavBarModelExtension> extensions = NavBarModelExtension.EP_NAME.getExtensionList();

    for (PsiElement e = normalize(psiElement), next = null; e != null; e = normalize(next), next = null) {
      // check if we're running circles due to getParent()->normalize/adjust()
      if (model.contains(e)) break;
      model.add(e);

      // check if a root is reached
      VirtualFile vFile = PsiUtilCore.getVirtualFile(e);
      if (roots.contains(vFile)) break;

      for (NavBarModelExtension ext : extensions) {
        PsiElement parent = ext.getParent(e);
        if (parent != null && parent != e) {
          //noinspection AssignmentToForLoopParameter
          next = parent;
          break;
        }
      }
    }
  }

  @Nullable
  protected static PsiElement normalize(@Nullable PsiElement e) {
    return NavBarModel.normalize(getOriginalElement(e));
  }

  @Nullable
  private static PsiElement getOriginalElement(@Nullable PsiElement e) {
    if (e == null || !e.isValid()) return null;

    PsiFile containingFile = e.getContainingFile();
    if (containingFile != null && containingFile.getVirtualFile() == null) return null;

    PsiElement orig = e.getOriginalElement();
    return !(e instanceof PsiCompiledElement) && orig instanceof PsiCompiledElement ? e : orig;
  }
}
