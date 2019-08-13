// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;

public class ReloadFromDiskAction extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (project == null || editor == null) return;
    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
    if (file == null) return;

    String message = IdeBundle.message("prompt.reload.file.from.disk", file.getVirtualFile().getPresentableUrl());
    int res = Messages.showOkCancelDialog(project, message, IdeBundle.message("title.reload.file"), Messages.getWarningIcon());
    if (res != Messages.OK) return;

    Runnable command = () -> ApplicationManager.getApplication().runWriteAction(
      () -> {
        if (!project.isDisposed()) {
          file.getVirtualFile().refresh(false, false);
          PsiManager.getInstance(project).reloadFromDisk(file);
        }
      }
    );
    CommandProcessor.getInstance().executeCommand(project, command, IdeBundle.message("command.reload.from.disk"), null);
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    boolean enabled = false;

    Project project = event.getProject();
    Editor editor = event.getData(CommonDataKeys.EDITOR);
    if (project != null && editor != null) {
      PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file != null && file.getVirtualFile() != null) {
        enabled = true;
      }
    }

    event.getPresentation().setEnabled(enabled);
  }
}
