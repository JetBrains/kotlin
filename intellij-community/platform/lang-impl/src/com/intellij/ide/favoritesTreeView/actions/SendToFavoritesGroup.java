// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.favoritesTreeView.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.ide.favoritesTreeView.FavoritesTreeNodeDescriptor;
import com.intellij.ide.favoritesTreeView.FavoritesTreeViewPanel;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SendToFavoritesGroup extends ActionGroup implements DumbAware {
  @Override
  @NotNull
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    if (e == null) {
      return EMPTY_ARRAY;
    }
    final Project project = e.getProject();
    final List<String> availableFavoritesLists = FavoritesManager.getInstance(project).getAvailableFavoritesListNames();
    availableFavoritesLists.remove(e.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY));
    if (availableFavoritesLists.isEmpty()) {
      return new AnAction[]{new SendToNewFavoritesListAction()};
    }

    List<AnAction> actions = new ArrayList<>();

    for (String list : availableFavoritesLists) {
      actions.add(new SendToFavoritesAction(list));
    }
    actions.add(Separator.getInstance());
    actions.add(new SendToNewFavoritesListAction());
    return actions.toArray(AnAction.EMPTY_ARRAY);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setVisible(SendToFavoritesAction.isEnabled(e)
                                   && e.getData(FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY) != null);
  }

  private static class SendToNewFavoritesListAction extends AnAction {
    SendToNewFavoritesListAction() {
      super(IdeBundle.message("action.send.to.new.favorites.list"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      final DataContext dataContext = e.getDataContext();
      Project project = e.getProject();
      FavoritesTreeNodeDescriptor[] roots = FavoritesTreeViewPanel.CONTEXT_FAVORITES_ROOTS_DATA_KEY.getData(dataContext);
      String listName = FavoritesTreeViewPanel.FAVORITES_LIST_NAME_DATA_KEY.getData(dataContext);

      String newName = AddNewFavoritesListAction.doAddNewFavoritesList(project);
      if (newName != null) {
        new SendToFavoritesAction(newName).doSend(FavoritesManager.getInstance(project), roots, listName);
      }
    }
  }
}
