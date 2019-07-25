// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.favoritesTreeView.FavoritesViewTreeBuilder;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesCompactEmptyMiddlePackagesAction extends FavoritesToolbarButtonAction {
  public FavoritesCompactEmptyMiddlePackagesAction(Project project, FavoritesViewTreeBuilder builder) {
    super(project, builder,
          IdeBundle.message("action.compact.empty.middle.packages"),
          AllIcons.ObjectBrowser.CompactEmptyPackages);
  }

  @Override
  public void updateButton(@NotNull AnActionEvent e) {
    super.updateButton(e);
    Presentation presentation = e.getPresentation();
    // see com.intellij.ide.projectView.impl.ProjectViewImpl.HideEmptyMiddlePackagesAction.update
    if (getViewSettings().isFlattenPackages()) {
      presentation.setText(IdeBundle.message("action.hide.empty.middle.packages"));
      presentation.setDescription(IdeBundle.message("action.show.hide.empty.middle.packages"));
    }
    else {
      presentation.setText(IdeBundle.message("action.compact.empty.middle.packages"));
      presentation.setDescription(IdeBundle.message("action.show.compact.empty.middle.packages"));
    }

  }

  @Override
  public boolean isOptionEnabled() {
    return getViewSettings().isHideEmptyMiddlePackages();
  }

  @Override
  public void setOption(boolean hide) {
    getViewSettings().setHideEmptyMiddlePackages(hide);
  }
}
