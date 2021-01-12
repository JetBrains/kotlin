// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.favoritesTreeView.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

class AddToNewFavoritesListAction extends AnAction implements DumbAware {
  AddToNewFavoritesListAction() {
    super(IdeBundle.messagePointer("action.add.to.new.favorites.list"),
          IdeBundle.messagePointer("action.add.to.new.favorites.list.description"), AllIcons.General.Add);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    Collection<AbstractTreeNode<?>> nodesToAdd = AddToFavoritesAction.getNodesToAdd(e.getDataContext(), true);
    if (!nodesToAdd.isEmpty()) {
      final String newName = AddNewFavoritesListAction.doAddNewFavoritesList(project);
      if (newName != null) {
        FavoritesManager.getInstance(project).addRoots(newName, nodesToAdd);
      }
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(AddToFavoritesAction.canCreateNodes(e));
  }
}
