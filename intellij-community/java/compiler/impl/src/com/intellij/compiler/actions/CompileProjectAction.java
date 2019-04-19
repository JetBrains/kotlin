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
package com.intellij.compiler.actions;

import com.intellij.history.LocalHistory;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.project.Project;
import com.intellij.task.ProjectTaskManager;
import com.intellij.task.ProjectTaskNotification;
import com.intellij.task.ProjectTaskResult;
import org.jetbrains.annotations.NotNull;

public class CompileProjectAction extends CompileActionBase {
  @Override
  protected void doAction(DataContext dataContext, final Project project) {
    ProjectTaskManager.getInstance(project).rebuildAllModules(new ProjectTaskNotification() {
      @Override
      public void finished(@NotNull ProjectTaskResult executionResult) {
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