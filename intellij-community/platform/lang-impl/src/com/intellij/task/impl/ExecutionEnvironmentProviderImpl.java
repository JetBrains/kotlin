// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
public final class ExecutionEnvironmentProviderImpl implements ExecutionEnvironmentProvider {
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
    return ProjectTaskRunner.EP_NAME.computeSafeIfAny(projectTaskRunner -> {
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

      return null;
    });
  }
}
