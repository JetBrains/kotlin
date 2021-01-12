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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.task.ProjectTaskManager;
import org.jetbrains.annotations.NotNull;

public class CompileDirtyAction extends CompileActionBase {

  @Override
  protected void doAction(DataContext dataContext, Project project) {
    ProjectTaskManager.getInstance(project).buildAllModules();
  }

  @Override
  public void update(@NotNull AnActionEvent e){
    super.update(e);
    Presentation presentation = e.getPresentation();
    if (!presentation.isEnabled()) {
      return;
    }
    presentation.setEnabled(e.getProject() != null);
  }
}