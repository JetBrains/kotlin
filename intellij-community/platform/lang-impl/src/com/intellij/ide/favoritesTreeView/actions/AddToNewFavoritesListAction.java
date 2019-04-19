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
    super(IdeBundle.message("action.add.to.new.favorites.list"),
          "Add To New Favorites List", AllIcons.General.Add);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    Collection<AbstractTreeNode> nodesToAdd = AddToFavoritesAction.getNodesToAdd(e.getDataContext(), true);
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
