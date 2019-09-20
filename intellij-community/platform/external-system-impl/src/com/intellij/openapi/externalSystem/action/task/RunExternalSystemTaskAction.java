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
package com.intellij.openapi.externalSystem.action.task;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.externalSystem.action.ExternalSystemActionUtil;
import com.intellij.openapi.externalSystem.action.ExternalSystemNodeAction;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskExecutionInfo;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class RunExternalSystemTaskAction extends ExternalSystemNodeAction<TaskData> {

  public RunExternalSystemTaskAction() {
    super(TaskData.class);
  }

  @Override
  protected void perform(@NotNull Project project,
                         @NotNull ProjectSystemId projectSystemId,
                         @NotNull TaskData taskData,
                         @NotNull AnActionEvent e) {
    final ExternalTaskExecutionInfo taskExecutionInfo = ExternalSystemActionUtil.buildTaskInfo(taskData);
    ExternalSystemUtil.runTask(taskExecutionInfo.getSettings(), taskExecutionInfo.getExecutorId(), project, projectSystemId);

    final DataContext dataContext = e.getDataContext();
    final ConfigurationContext context = ConfigurationContext.getFromContext(dataContext);
    RunnerAndConfigurationSettings configuration = context.findExisting();
    RunManager runManager = context.getRunManager();
    if (configuration == null) {
      configuration = context.getConfiguration();
      if (configuration == null) {
        return;
      }
      runManager.setTemporaryConfiguration(configuration);
    }
    runManager.setSelectedConfiguration(configuration);
  }
}
