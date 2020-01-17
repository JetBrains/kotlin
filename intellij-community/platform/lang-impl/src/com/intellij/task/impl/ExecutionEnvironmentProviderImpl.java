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
package com.intellij.task.impl;

import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.task.ExecuteRunConfigurationTask;
import com.intellij.task.ProjectTaskRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class ExecutionEnvironmentProviderImpl implements ExecutionEnvironmentProvider {

  private static final Logger LOG = Logger.getInstance(ExecutionEnvironmentProvider.class);

  @Nullable
  @Override
  public ExecutionEnvironment createExecutionEnvironment(@NotNull Project project,
                                                         @NotNull RunProfile runProfile,
                                                         @NotNull Executor executor,
                                                         @NotNull ExecutionTarget target,
                                                         @Nullable RunnerSettings runnerSettings,
                                                         @Nullable ConfigurationPerRunnerSettings configurationSettings,
                                                         @Nullable RunnerAndConfigurationSettings settings) {

    ExecuteRunConfigurationTask
      runTask = new ExecuteRunConfigurationTaskImpl(runProfile, target, runnerSettings, configurationSettings, settings);
    for (ProjectTaskRunner projectTaskRunner : ProjectTaskRunner.EP_NAME.getExtensions()) {
      try {
        if (projectTaskRunner.canRun(project, runTask)) {
          return projectTaskRunner.createExecutionEnvironment(project, runTask, executor);
        }
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Exception e) {
        LOG.error("Broken project task runner: " + projectTaskRunner.getClass().getName(), e);
      }
    }
    return null;
  }
}
