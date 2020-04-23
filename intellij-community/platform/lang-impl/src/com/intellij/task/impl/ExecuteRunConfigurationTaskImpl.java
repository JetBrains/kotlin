// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.task.impl;

import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.lang.LangBundle;
import com.intellij.task.ExecuteRunConfigurationTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public final class ExecuteRunConfigurationTaskImpl extends AbstractProjectTask implements ExecuteRunConfigurationTask {
  @NotNull private final RunProfile myRunProfile;
  @Nullable private ExecutionTarget myTarget;
  @Nullable private RunnerSettings myRunnerSettings;
  @Nullable private ConfigurationPerRunnerSettings myConfigurationSettings;
  @Nullable private RunnerAndConfigurationSettings mySettings;

  public ExecuteRunConfigurationTaskImpl(@NotNull RunProfile runProfile) {
    myRunProfile = runProfile;
  }

  public ExecuteRunConfigurationTaskImpl(@NotNull RunProfile runProfile,
                                         @NotNull ExecutionTarget target,
                                         @Nullable RunnerSettings runnerSettings,
                                         @Nullable ConfigurationPerRunnerSettings configurationSettings,
                                         @Nullable RunnerAndConfigurationSettings settings) {
    myRunProfile = runProfile;
    myTarget = target;
    myRunnerSettings = runnerSettings;
    myConfigurationSettings = configurationSettings;
    mySettings = settings;
  }

  @NotNull
  @Override
  public RunProfile getRunProfile() {
    return myRunProfile;
  }

  @Nullable
  @Override
  public ExecutionTarget getExecutionTarget() {
    return myTarget;
  }

  @Nullable
  @Override
  public RunnerSettings getRunnerSettings() {
    return myRunnerSettings;
  }

  @Nullable
  @Override
  public ConfigurationPerRunnerSettings getConfigurationSettings() {
    return myConfigurationSettings;
  }

  @Nullable
  @Override
  public RunnerAndConfigurationSettings getSettings() {
    return mySettings;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return LangBundle.message("project.task.name.run.task.0", myRunProfile.getName());
  }
}
