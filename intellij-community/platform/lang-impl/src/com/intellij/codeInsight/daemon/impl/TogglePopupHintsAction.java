/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

public class TogglePopupHintsAction extends AnAction {
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.actions.TogglePopupHintsAction");

  private static PsiFile getTargetFile(@NotNull DataContext dataContext) {
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return null;
    }
    VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
    if (files.length == 0) {
      return null;
    }
    PsiFile psiFile = PsiManager.getInstance(project).findFile(files[0]);
    LOG.assertTrue(psiFile != null);
    return psiFile;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    PsiFile psiFile = getTargetFile(e.getDataContext());
    e.getPresentation().setEnabled(psiFile != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    PsiFile psiFile = getTargetFile(e.getDataContext());
    LOG.assertTrue(psiFile != null);
    Project project = e.getProject();
    LOG.assertTrue(project != null);
    DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
    codeAnalyzer.setImportHintsEnabled(psiFile, !codeAnalyzer.isImportHintsEnabled(psiFile));
    DaemonListeners.getInstance(project).updateStatusBar();
  }
}
