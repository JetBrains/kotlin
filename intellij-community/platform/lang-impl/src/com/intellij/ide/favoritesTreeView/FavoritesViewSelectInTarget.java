// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.SelectInManager;
import com.intellij.ide.StandardTargetWeights;
import com.intellij.ide.impl.SelectInTargetPsiWrapper;
import com.intellij.notebook.editor.BackedVirtualFile;
import com.intellij.openapi.extensions.ExtensionNotApplicableException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.PlatformUtils;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author anna
 * @author Konstantin Bulenkov
 */
public class FavoritesViewSelectInTarget extends SelectInTargetPsiWrapper {
  public FavoritesViewSelectInTarget(final Project project) {
    super(project);

    if (PlatformUtils.isPyCharmEducational()) {
      throw ExtensionNotApplicableException.INSTANCE;
    }
  }

  public String toString() {
    return SelectInManager.FAVORITES;
  }

  @Override
  public String getToolWindowId() {
    return SelectInManager.FAVORITES;
  }

  @Override
  protected void select(Object selector, VirtualFile virtualFile, boolean requestFocus) {
    select(myProject, selector, virtualFile, requestFocus);
  }

  @Override
  protected void select(PsiElement element, boolean requestFocus) {
    PsiElement toSelect = findElementToSelect(element, null);
    if (toSelect != null) {
      VirtualFile virtualFile = PsiUtilCore.getVirtualFile(toSelect);
      virtualFile = BackedVirtualFile.getOriginFileIfBacked(virtualFile);
      select(toSelect, virtualFile, requestFocus);
    }
  }

  private static ActionCallback select(@NotNull Project project, Object toSelect, VirtualFile virtualFile, boolean requestFocus) {
    final ActionCallback result = new ActionCallback();

    ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
    final ToolWindow favoritesToolWindow = windowManager.getToolWindow(ToolWindowId.FAVORITES_VIEW);

    if (favoritesToolWindow != null) {
      final Runnable runnable = () -> {
        final FavoritesTreeViewPanel panel = UIUtil.findComponentOfType(favoritesToolWindow.getComponent(), FavoritesTreeViewPanel.class);
        if (panel != null) {
          panel.selectElement(toSelect, virtualFile, requestFocus);
          result.setDone();
        }
      };

      if (requestFocus) {
        favoritesToolWindow.activate(runnable, false);
      }
      else {
        favoritesToolWindow.show(runnable);
      }
    }

    return result;
  }

  @Override
  protected boolean canSelect(final PsiFileSystemItem file) {
    return findSuitableFavoritesList(file.getVirtualFile(), myProject, null) != null;
  }

  public static String findSuitableFavoritesList(VirtualFile file, Project project, final String currentSubId) {
    return FavoritesManager.getInstance(project).getFavoriteListName(currentSubId, file);
  }

  @Override
  public String getMinorViewId() {
    return FavoritesProjectViewPane.ID;
  }

  @Override
  public float getWeight() {
    return StandardTargetWeights.FAVORITES_WEIGHT;
  }

}