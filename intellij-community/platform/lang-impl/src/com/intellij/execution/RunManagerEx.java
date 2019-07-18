// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class RunManagerEx extends RunManager {
  @NotNull
  public static RunManagerEx getInstanceEx(@NotNull Project project) {
    return (RunManagerEx)RunManager.getInstance(project);
  }

  /**
   * @deprecated Use {@link #setSelectedConfiguration(RunnerAndConfigurationSettings)} instead
   */
  @Deprecated
  public final void setActiveConfiguration(@Nullable RunnerAndConfigurationSettings configuration) {
    setSelectedConfiguration(configuration);
  }

  @Deprecated
  public final void addConfiguration(RunnerAndConfigurationSettings settings, boolean isShared, List<BeforeRunTask> tasks, boolean addTemplateTasksIfAbsent) {
    settings.setShared(isShared);
    setBeforeRunTasks(settings.getConfiguration(), tasks, addTemplateTasksIfAbsent);
    addConfiguration(settings);
  }

  @NotNull
  public abstract List<BeforeRunTask> getBeforeRunTasks(@NotNull RunConfiguration configuration);

  public abstract void setBeforeRunTasks(@NotNull RunConfiguration configuration, @NotNull List<BeforeRunTask> tasks);

  @Deprecated
  public final void setBeforeRunTasks(@NotNull RunConfiguration configuration, @NotNull List<BeforeRunTask> tasks, @SuppressWarnings("unused") boolean addEnabledTemplateTasksIfAbsent) {
    setBeforeRunTasks(configuration, tasks);
  }

  @NotNull
  public abstract <T extends BeforeRunTask> List<T> getBeforeRunTasks(@NotNull RunConfiguration settings, Key<T> taskProviderId);

  @NotNull
  public abstract <T extends BeforeRunTask> List<T> getBeforeRunTasks(Key<T> taskProviderId);

  public Icon getConfigurationIcon(@NotNull RunnerAndConfigurationSettings settings) {
    return getConfigurationIcon(settings, false);
  }

  @NotNull
  public abstract Icon getConfigurationIcon(@NotNull RunnerAndConfigurationSettings settings, boolean withLiveIndicator);

  /**
   * @deprecated Use {@link #getAllSettings()}
   */
  @NotNull
  @Deprecated
  public final Collection<RunnerAndConfigurationSettings> getSortedConfigurations() {
    return getAllSettings();
  }

  /**
   * @deprecated Use {@link RunManagerListener#TOPIC} instead.
   */
  @Deprecated
  public void addRunManagerListener(@NotNull RunManagerListener listener) {
  }

  @SafeVarargs
  public static void disableTasks(Project project, RunConfiguration settings, @NotNull Key<? extends BeforeRunTask>... keys) {
    for (Key<? extends BeforeRunTask> key : keys) {
      List<? extends BeforeRunTask> tasks = getInstanceEx(project).getBeforeRunTasks(settings, key);
      for (BeforeRunTask task : tasks) {
        task.setEnabled(false);
      }
    }
  }

  @SafeVarargs
  public static int getTasksCount(Project project, RunConfiguration settings, @NotNull Key<? extends BeforeRunTask>... keys) {
    return Arrays.stream(keys).mapToInt(key -> getInstanceEx(project).getBeforeRunTasks(settings, key).size()).sum();
  }
}