// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.actions;

import com.intellij.history.LocalHistory;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskManager;
import com.intellij.task.ProjectTaskNotification;
import com.intellij.task.ProjectTaskResult;
import org.jetbrains.annotations.NotNull;

public class CompileProjectAction extends CompileActionBase {
  @Override
  protected void doAction(DataContext dataContext, final Project project) {
    ProjectTaskManager.getInstance(project).rebuildAllModules(new ProjectTaskNotification() {
      @Override
      public void finished(@NotNull ProjectTaskContext context, @NotNull ProjectTaskResult executionResult) {
        if (executionResult.isAborted() || project.isDisposed()) {
          return;
        }

        String text = getTemplatePresentation().getText();
        LocalHistory.getInstance().putSystemLabel(
          project, CompilerBundle
            .message(executionResult.getErrors() == 0 ? "rebuild.lvcs.label.no.errors" : "rebuild.lvcs.label.with.errors", text));
      }
    });
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    Presentation presentation = e.getPresentation();
    if (!presentation.isEnabled()) {
      return;
    }
    presentation.setEnabled(e.getProject() != null);
  }
}