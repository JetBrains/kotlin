// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.favoritesTreeView.actions;

import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.favoritesTreeView.FavoritesTreeViewPanel;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AddToFavoritesActionGroup extends ActionGroup implements DumbAware {

  @Override
  @NotNull
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    if (e == null) return AnAction.EMPTY_ARRAY;
    final Project project = e.getProject();
    if (project == null) {
      return AnAction.EMPTY_ARRAY;
    }
    final List<String> availableFavoritesLists = FavoritesManager.getInstance(project).getAvailableFavoritesListNames();
    availableFavoritesLists.remove(e.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY));
    if (availableFavoritesLists.isEmpty()) {
      return new AnAction[]{new AddToNewFavoritesListAction()};
    }

    AnAction[] actions = new AnAction[availableFavoritesLists.size() + 2];
    int idx = 0;
    for (String favoritesList : availableFavoritesLists) {
      actions[idx++] = new AddToFavoritesAction(favoritesList);
    }
    actions[idx++] = Separator.getInstance();
    actions[idx] = new AddToNewFavoritesListAction();
    return actions;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    try {
      e.getPresentation().setVisible(AddToFavoritesAction.canCreateNodes(e));
    }
    catch (IndexNotReadyException e1) {
      e.getPresentation().setVisible(false);
    }
  }
}
