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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.action.ExternalSystemActionUtil;
import com.intellij.openapi.externalSystem.action.ExternalSystemNodeAction;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskExecutionInfo;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class RunExternalSystemTaskAction extends ExternalSystemNodeAction<TaskData> {

  private static final Logger LOG = Logger.getInstance(RunExternalSystemTaskAction.class);

  public RunExternalSystemTaskAction() {
    super(TaskData.class);
  }

  @Override
  protected void perform(@NotNull Project project,
                         @NotNull ProjectSystemId projectSystemId,
                         @NotNull TaskData taskData,
                         @NotNull AnActionEvent e) {
    final ExternalTaskExecutionInfo taskExecutionInfo = ExternalSystemActionUtil.buildTaskInfo(taskData);
    final ConfigurationContext context = ConfigurationContext.getFromContext(e.getDataContext());

    RunnerAndConfigurationSettings configuration = findOrGet(context);
    if (configuration == null ||
        !runTaskAsExistingConfiguration(project, projectSystemId, taskExecutionInfo, configuration)) {
      runTaskAsNewRunConfiguration(project, projectSystemId, taskExecutionInfo);
      configuration = findOrGet(context); // if created during runTaskAsNewRunConfiguration
    }

    context.getRunManager().setSelectedConfiguration(configuration);
  }

  @Nullable
  private static RunnerAndConfigurationSettings findOrGet(@NotNull ConfigurationContext context) {
    RunnerAndConfigurationSettings result = context.findExisting();
    if (result == null) {
      result = context.getConfiguration();
      if (result != null) {
        context.getRunManager().setTemporaryConfiguration(result);
      }
    }
    return result;
  }

  private static boolean runTaskAsExistingConfiguration(@NotNull Project project,
                                                        @NotNull ProjectSystemId projectSystemId,
                                                        @NotNull ExternalTaskExecutionInfo taskExecutionInfo,
                                                        @NotNull RunnerAndConfigurationSettings configuration) {
    final String executorId = taskExecutionInfo.getExecutorId();
    String runnerId = ExternalSystemUtil.getRunnerId(executorId);
    if (runnerId == null) {
      return false;
    }
    Executor executor = ExecutorRegistry.getInstance().getExecutorById(executorId);
    ProgramRunner<?> runner = ProgramRunner.findRunnerById(runnerId);

    if (executor == null || runner == null) {
      return false;
    }

    ExecutionEnvironment environment = new ExecutionEnvironment(executor, runner, configuration, project);
    ApplicationManager.getApplication().invokeLater(() -> {
      try {
        environment.getRunner().execute(environment);
      }
      catch (ExecutionException exception) {
        LOG.error("Failed to execute " + projectSystemId.getReadableName() + " task.", exception);
      }
    });

    return true;
  }

  private static void runTaskAsNewRunConfiguration(@NotNull Project project,
                                                   @NotNull ProjectSystemId projectSystemId,
                                                   @NotNull ExternalTaskExecutionInfo taskExecutionInfo) {
    ExternalSystemUtil.runTask(taskExecutionInfo.getSettings(), taskExecutionInfo.getExecutorId(), project, projectSystemId);
  }
}
