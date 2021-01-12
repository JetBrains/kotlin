/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.ide.favoritesTreeView.FavoritesManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AddAllOpenFilesToFavorites extends AnAction implements DumbAware {
  private final String myFavoritesName;

  public AddAllOpenFilesToFavorites(String chosenList) {
    getTemplatePresentation().setText(chosenList, false);
    myFavoritesName = chosenList;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) {
      return;
    }

    final FavoritesManager favoritesManager = FavoritesManager.getInstance(project);

    final ArrayList<PsiFile> filesToAdd = getFilesToAdd(project);
    for (PsiFile file : filesToAdd) {
      favoritesManager.addRoots(myFavoritesName, null, file);
    }
  }

  static ArrayList<PsiFile> getFilesToAdd(Project project) {
    ArrayList<PsiFile> result = new ArrayList<>();
    final FileEditorManager editorManager = FileEditorManager.getInstance(project);
    final PsiManager psiManager = PsiManager.getInstance(project);
    for (VirtualFile openFile : editorManager.getOpenFiles()) {
      if (!openFile.isValid()) continue;
      final PsiFile psiFile = psiManager.findFile(openFile);
      if (psiFile != null) {
        result.add(psiFile);
      }
    }
    return result;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) {
      e.getPresentation().setEnabled(false);
      return;
    }
    e.getPresentation().setEnabled(!getFilesToAdd(project).isEmpty());
  }
}
