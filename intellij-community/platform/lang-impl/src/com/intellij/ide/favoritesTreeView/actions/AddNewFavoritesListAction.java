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

import com.intellij.ide.IdeBundle;
import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AddNewFavoritesListAction extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project != null) {
      doAddNewFavoritesList(project);
    }
  }

  public static String doAddNewFavoritesList(final Project project) {
    final FavoritesManager favoritesManager = FavoritesManager.getInstance(project);
    final String name = Messages.showInputDialog(project,
                                                 IdeBundle.message("prompt.input.new.favorites.list.name"),
                                                 IdeBundle.message("title.add.new.favorites.list"),
                                                 Messages.getInformationIcon(),
                                                 getUniqueName(project), new InputValidator() {
      @Override
      public boolean checkInput(String inputString) {
        return inputString != null && inputString.trim().length() > 0;
      }

      @Override
      public boolean canClose(String inputString) {
        inputString = inputString.trim();
        if (favoritesManager.getAvailableFavoritesListNames().contains(inputString)) {
          Messages.showErrorDialog(project,
                                   IdeBundle.message("error.favorites.list.already.exists", inputString.trim()),
                                   IdeBundle.message("title.unable.to.add.favorites.list"));
          return false;
        }
        return inputString.length() > 0;
      }
    });
    if (name == null || name.length() == 0) return null;
    favoritesManager.createNewList(name);
    return name;
  }

  private static String getUniqueName(Project project) {
    List<String> names = FavoritesManager.getInstance(project).getAvailableFavoritesListNames();
    for (int i = 0; ; i++) {
      String newName = IdeBundle.message("favorites.list.unnamed", i > 0 ? i : "");
      if (names.contains(newName)) continue;
      return newName;
    }
  }
}
