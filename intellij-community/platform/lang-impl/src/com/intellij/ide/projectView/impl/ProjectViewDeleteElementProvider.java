// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.history.LocalHistory;
import com.intellij.history.LocalHistoryAction;
import com.intellij.ide.DeleteProvider;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper;
import com.intellij.ide.util.DeleteHandler;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ProjectViewDeleteElementProvider implements DeleteProvider {

  @Override
  public boolean canDeleteElement(@NotNull DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) return false;

    final PsiElement[] elements = getElementsToDelete(project, dataContext);
    return DeleteHandler.shouldEnableDeleteAction(elements);
  }

  @Override
  public void deleteElement(@NotNull DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) return;

    List<PsiElement> validElements = new ArrayList<>();
    for (PsiElement psiElement : getElementsToDelete(project, dataContext)) {
      if (psiElement != null && psiElement.isValid()) {
        validElements.add(psiElement);
      }
    }

    PsiElement[] elements = PsiUtilCore.toPsiElementArray(validElements);
    LocalHistoryAction a = LocalHistory.getInstance().startAction(IdeBundle.message("progress.deleting"));
    try {
      DeleteHandler.deletePsiElement(elements, project);
    }
    finally {
      a.finish();
    }
  }

  protected abstract PsiElement @NotNull [] getSelectedPSIElements(@NotNull DataContext dataContext);
  protected abstract Boolean hideEmptyMiddlePackages(@NotNull DataContext dataContext);

  private PsiElement @NotNull [] getElementsToDelete(@NotNull Project project, @NotNull DataContext dataContext) {
    PsiElement[] elements = getSelectedPSIElements(dataContext);
    for (int idx = 0; idx < elements.length; idx++) {
      final PsiElement element = elements[idx];
      if (element instanceof PsiDirectory) {
        PsiDirectory directory = (PsiDirectory)element;
        final ProjectViewDirectoryHelper directoryHelper = ProjectViewDirectoryHelper.getInstance(project);
        if (hideEmptyMiddlePackages(dataContext) &&
            directory.getChildren().length == 0 &&
            !directoryHelper.skipDirectory(directory)) {
          while (true) {
            PsiDirectory parent = directory.getParentDirectory();
            if (parent == null) break;
            if (directoryHelper.skipDirectory(parent) ||
                PsiDirectoryFactory.getInstance(project).getQualifiedName(parent, false).isEmpty()) {
              break;
            }
            PsiElement[] children = parent.getChildren();
            if (children.length == 0 || children.length == 1 && children[0] == directory) {
              directory = parent;
            }
            else {
              break;
            }
          }
          elements[idx] = directory;
        }
        final VirtualFile virtualFile = directory.getVirtualFile();
        final String path = virtualFile.getPath();
        if (path.endsWith(JarFileSystem.JAR_SEPARATOR)) { // if is jar-file root
          final VirtualFile vFile =
            LocalFileSystem.getInstance().findFileByPath(path.substring(0, path.length() - JarFileSystem.JAR_SEPARATOR.length()));
          if (vFile != null) {
            final PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
            if (psiFile != null) {
              elements[idx] = psiFile;
            }
          }
        }
      }
    }
    return elements;
  }
}
