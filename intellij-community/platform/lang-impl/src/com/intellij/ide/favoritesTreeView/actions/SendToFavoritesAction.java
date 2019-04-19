// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.favoritesTreeView.actions;

import com.intellij.ide.favoritesTreeView.FavoritesListNode;
import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.favoritesTreeView.FavoritesTreeNodeDescriptor;
import com.intellij.ide.favoritesTreeView.FavoritesTreeViewPanel;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * @author anna
 * @author Konstantin Bulenkov
 */
public class SendToFavoritesAction extends AnAction implements DumbAware {
  private final String toName;

  public SendToFavoritesAction(String name) {
    super(name);
    toName = name;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    Project project = e.getProject();
    final FavoritesManager favoritesManager = FavoritesManager.getInstance(project);

    FavoritesTreeNodeDescriptor[] roots = FavoritesTreeViewPanel.CONTEXT_FAVORITES_ROOTS_DATA_KEY.getData(dataContext);
    if (roots == null) return;

    for (FavoritesTreeNodeDescriptor root : roots) {
      FavoritesTreeNodeDescriptor listNode = root.getFavoritesRoot();
      if (listNode != null && listNode !=root && listNode.getElement() instanceof FavoritesListNode) {
        doSend(favoritesManager, new FavoritesTreeNodeDescriptor[]{root}, listNode.getElement().getName());
      }
    }
  }

  public void doSend(final FavoritesManager favoritesManager, final FavoritesTreeNodeDescriptor[] roots, final String listName) {
    for (FavoritesTreeNodeDescriptor root : roots) {
      final AbstractTreeNode rootElement = root.getElement();
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
    FavoritesTreeNodeDescriptor[] roots = e.getData(FavoritesTreeViewPanel.CONTEXT_FAVORITES_ROOTS_DATA_KEY);
    if (roots == null || roots.length == 0) {
      return false;
    }
    for (FavoritesTreeNodeDescriptor root : roots) {
      FavoritesTreeNodeDescriptor listNode = root.getFavoritesRoot();
      if (listNode == null || listNode ==root || !(listNode.getElement() instanceof FavoritesListNode))
        return false;
    }
    return true;
  }
}
