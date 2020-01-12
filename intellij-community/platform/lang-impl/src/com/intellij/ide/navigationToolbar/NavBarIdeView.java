// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.navigationToolbar;

import com.intellij.ide.IdeView;
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
* @author Konstantin Bulenkov
*/
public final class NavBarIdeView implements IdeView {
  private final NavBarPanel myPanel;

  public NavBarIdeView(NavBarPanel panel) {
    myPanel = panel;
  }

  @Override
  public void selectElement(PsiElement element) {
    myPanel.getModel().updateModel(element, null);

    if (element instanceof Navigatable) {
      final Navigatable navigatable = (Navigatable)element;
      if (navigatable.canNavigate()) {
        ((Navigatable)element).navigate(true);
      }
    }
    myPanel.hideHint();
  }

  @Override
  public PsiDirectory @NotNull [] getDirectories() {
    NavBarPopup nodePopup = myPanel.getNodePopup();
    JBIterable<?> selection =
      nodePopup != null && nodePopup.isVisible() ? JBIterable.from(nodePopup.getList().getSelectedValuesList()) :
      myPanel.getSelection();
    List<PsiDirectory> dirs = selection.flatMap(
      o -> {
        if (o instanceof PsiElement && !((PsiElement)o).isValid()) return JBIterable.empty();
        if (o instanceof PsiDirectory) return JBIterable.of((PsiDirectory)o);
        if (o instanceof PsiDirectoryContainer) {
          return JBIterable.of(((PsiDirectoryContainer)o).getDirectories());
        }
        if (o instanceof PsiElement) {
          PsiFile file = ((PsiElement)o).getContainingFile();
          return JBIterable.of(file != null ? file.getContainingDirectory() : null);
        }
        if (o instanceof Module && !((Module)o).isDisposed()) {
          PsiManager psiManager = PsiManager.getInstance(myPanel.getProject());
          return JBIterable.of(ModuleRootManager.getInstance((Module)o).getSourceRoots())
            .filterMap(file -> psiManager.findDirectory(file));
        }
        return JBIterable.empty();
      })
      .filter(o -> o.isValid())
      .toList();
    return dirs.isEmpty() ? PsiDirectory.EMPTY_ARRAY : dirs.toArray(PsiDirectory.EMPTY_ARRAY);
  }

  @Override
  public PsiDirectory getOrChooseDirectory() {
    return DirectoryChooserUtil.getOrChooseDirectory(this);
  }
}
