// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView.actions;

import com.intellij.ide.favoritesTreeView.FavoriteTreeNodeDescriptor;
import com.intellij.ide.favoritesTreeView.FavoritesListNode;
import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.favoritesTreeView.FavoritesTreeViewPanel;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public final class SendToFavoritesAction extends AnAction implements DumbAware {
  private final String toName;

  public SendToFavoritesAction(String name) {
    super(name);
    toName = name;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    Project project = e.getProject();
    FavoritesManager favoritesManager = FavoritesManager.getInstance(project);

    FavoriteTreeNodeDescriptor[] roots = FavoritesTreeViewPanel.CONTEXT_FAVORITES_ROOTS_DATA_KEY.getData(dataContext);
    if (roots == null) {
      return;
    }

    for (FavoriteTreeNodeDescriptor root : roots) {
      FavoriteTreeNodeDescriptor listNode = root.getFavoritesRoot();
      if (listNode != null && listNode !=root && listNode.getElement() instanceof FavoritesListNode) {
        doSend(favoritesManager, new FavoriteTreeNodeDescriptor[]{root}, listNode.getElement().getName());
      }
    }
  }

  public void doSend(FavoritesManager favoritesManager, FavoriteTreeNodeDescriptor[] roots, final String listName) {
    for (FavoriteTreeNodeDescriptor root : roots) {
      AbstractTreeNode<?> rootElement = root.getElement();
      String name = listName;
      if (name == null) {
        name = root.getFavoritesRoot().getName();
      }
      favoritesManager.removeRoot(name, Collections.singletonList(rootElement));
      favoritesManager.addRoots(toName, Collections.singletonList(rootElement));
    }
  }


  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(isEnabled(e));
  }

  static boolean isEnabled(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return false;
    }
    FavoriteTreeNodeDescriptor[] roots = e.getData(FavoritesTreeViewPanel.CONTEXT_FAVORITES_ROOTS_DATA_KEY);
    if (roots == null || roots.length == 0) {
      return false;
    }
    for (FavoriteTreeNodeDescriptor root : roots) {
      FavoriteTreeNodeDescriptor listNode = root.getFavoritesRoot();
      if (listNode == null || listNode ==root || !(listNode.getElement() instanceof FavoritesListNode))
        return false;
    }
    return true;
  }
}
