// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.actions;

import com.intellij.history.LocalHistory;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.task.ProjectTaskManager;
import org.jetbrains.annotations.NotNull;

public class CompileProjectAction extends CompileActionBase {
  @Override
  protected void doAction(DataContext dataContext, final Project project) {
    ProjectTaskManager.getInstance(project)
      .rebuildAllModules()
      .onSuccess(result -> {
        if (!result.isAborted() && !project.isDisposed()) {
          String text = getTemplatePresentation().getText();
          LocalHistory.getInstance().putSystemLabel(
            project, CompilerBundle.message(result.hasErrors() ? "rebuild.lvcs.label.with.errors" : "rebuild.lvcs.label.no.errors", text));
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