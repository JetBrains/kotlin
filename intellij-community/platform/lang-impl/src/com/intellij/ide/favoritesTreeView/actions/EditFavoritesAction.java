// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView.actions;

import com.intellij.ide.favoritesTreeView.FavoritesListProvider;
import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.favoritesTreeView.FavoritesTreeViewPanel;
import com.intellij.ide.favoritesTreeView.FavoritesViewTreeBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CommonActionsPanel;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class EditFavoritesAction extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    FavoritesViewTreeBuilder treeBuilder = e.getData(FavoritesTreeViewPanel.FAVORITES_TREE_BUILDER_KEY);
    String listName = e.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY);
    if (project == null || treeBuilder == null || listName == null) {
      return;
    }
    FavoritesManager favoritesManager = FavoritesManager.getInstance(project);
    FavoritesListProvider provider = favoritesManager.getListProvider(listName);
    Set<Object> selection = treeBuilder.getSelectedElements();
    if (provider != null && provider.willHandle(CommonActionsPanel.Buttons.EDIT, project, selection)) {
      provider.handle(CommonActionsPanel.Buttons.EDIT, project, selection, treeBuilder.getTree());
      return;
    }
    favoritesManager.renameList(project, listName);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setText(getTemplatePresentation().getText());
    e.getPresentation().setIcon(CommonActionsPanel.Buttons.EDIT.getIcon());
    e.getPresentation().setEnabled(true);
    Project project = e.getProject();
    FavoritesViewTreeBuilder treeBuilder = e.getData(FavoritesTreeViewPanel.FAVORITES_TREE_BUILDER_KEY);
    String listName = e.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY);
    if (project == null || treeBuilder == null || listName == null) {
      e.getPresentation().setEnabled(false);
      return;
    }
    FavoritesManager favoritesManager = FavoritesManager.getInstance(project);
    FavoritesListProvider provider = favoritesManager.getListProvider(listName);
    Set<Object> selection = treeBuilder.getSelectedElements();
    if (provider != null) {
      e.getPresentation().setEnabled(provider.willHandle(CommonActionsPanel.Buttons.EDIT, project, selection));
      e.getPresentation().setText(provider.getCustomName(CommonActionsPanel.Buttons.EDIT));
    }
  }
}
