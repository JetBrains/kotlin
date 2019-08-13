/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
    myPanel.getModel().updateModel(element);

    if (element instanceof Navigatable) {
      final Navigatable navigatable = (Navigatable)element;
      if (navigatable.canNavigate()) {
        ((Navigatable)element).navigate(true);
      }
    }
    myPanel.hideHint();
  }

  @NotNull
  @Override
  public PsiDirectory[] getDirectories() {
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
