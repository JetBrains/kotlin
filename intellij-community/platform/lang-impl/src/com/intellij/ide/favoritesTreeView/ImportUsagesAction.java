// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageView;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ImportUsagesAction extends AnAction {
  public ImportUsagesAction() {
    super("To Favorites", "To Favorites", AllIcons.Toolwindows.ToolWindowFavorites);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final DataContext dc = e.getDataContext();
    final boolean enabled = isEnabled(dc);
    e.getPresentation().setEnabled(enabled);
  }

  private boolean isEnabled(DataContext dc) {
    final Project project = CommonDataKeys.PROJECT.getData(dc);
    final Usage[] usages = UsageView.USAGES_KEY.getData(dc);
    return project != null && usages != null && usages.length > 0;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final DataContext dc = e.getDataContext();
    final boolean enabled = isEnabled(dc);
    if (!enabled) return;

    final Project project = CommonDataKeys.PROJECT.getData(dc);

    final Collection<AbstractTreeNode<?>> nodes = new UsageFavoriteNodeProvider().getFavoriteNodes(dc, ViewSettings.DEFAULT);
    final FavoritesManager favoritesManager = FavoritesManager.getInstance(project);
    if (nodes != null && !nodes.isEmpty()) {
      favoritesManager.addRoots(TaskDefaultFavoriteListProvider.CURRENT_TASK, nodes);
    }
  }
}
