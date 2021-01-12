// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.execution;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunConfigurationBeforeRunProvider;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskPojo;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public abstract class ExternalSystemBeforeRunTaskProvider extends BeforeRunTaskProvider<ExternalSystemBeforeRunTask> {
  @NotNull private final ProjectSystemId mySystemId;
  @NotNull private final Project myProject;
  @NotNull private final Key<ExternalSystemBeforeRunTask> myId;

  public ExternalSystemBeforeRunTaskProvider(@NotNull ProjectSystemId systemId,
                                             @NotNull Project project,
                                             @NotNull Key<ExternalSystemBeforeRunTask> id) {
    mySystemId = systemId;
    myProject = project;
    myId = id;
  }

  @Override
  @NotNull
  public Key<ExternalSystemBeforeRunTask> getId() {
    return myId;
  }

  @Override
  public String getName() {
    return ExternalSystemBundle.message("tasks.before.run.empty", mySystemId.getReadableName());
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  public boolean configureTask(@NotNull RunConfiguration runConfiguration, @NotNull ExternalSystemBeforeRunTask task) {
    ExternalSystemEditTaskDialog dialog = new ExternalSystemEditTaskDialog(myProject, task.getTaskExecutionSettings(), mySystemId);
    dialog.setTitle(ExternalSystemBundle.message("tasks.select.task.title", mySystemId.getReadableName()));

    if (!dialog.showAndGet()) {
      return false;
    }

    return true;
  }

  @Override
  public boolean canExecuteTask(@NotNull RunConfiguration configuration, @NotNull ExternalSystemBeforeRunTask beforeRunTask) {
    final ExternalSystemTaskExecutionSettings executionSettings = beforeRunTask.getTaskExecutionSettings();

    final List<ExternalTaskPojo> tasks = new ArrayList<>();
    for (String taskName : executionSettings.getTaskNames()) {
      tasks.add(new ExternalTaskPojo(taskName, executionSettings.getExternalProjectPath(), null));
    }
    if (tasks.isEmpty()) return true;


    ExecutionEnvironment environment =
      ExternalSystemUtil.createExecutionEnvironment(myProject, mySystemId, executionSettings, DefaultRunExecutor.EXECUTOR_ID);
    if (environment == null) return false;

    return environment.getRunner().canRun(DefaultRunExecutor.EXECUTOR_ID, environment.getRunProfile());
  }

  @Override
  public boolean executeTask(@NotNull DataContext context,
                             @NotNull RunConfiguration configuration,
                             @NotNull ExecutionEnvironment env,
                             @NotNull ExternalSystemBeforeRunTask beforeRunTask) {

    final ExternalSystemTaskExecutionSettings executionSettings = beforeRunTask.getTaskExecutionSettings();

    final List<ExternalTaskPojo> tasks = new ArrayList<>();
    for (String taskName : executionSettings.getTaskNames()) {
      tasks.add(new ExternalTaskPojo(taskName, executionSettings.getExternalProjectPath(), null));
    }
    if (tasks.isEmpty()) return true;

    ExecutionEnvironment environment =
      ExternalSystemUtil.createExecutionEnvironment(myProject, mySystemId, executionSettings, DefaultRunExecutor.EXECUTOR_ID);
    if (environment == null) return false;

    final ProgramRunner runner = environment.getRunner();
    environment.setExecutionId(env.getExecutionId());

    return RunConfigurationBeforeRunProvider.doRunTask(DefaultRunExecutor.getRunExecutorInstance().getId(), environment, runner);
  }

  @Override
  public String getDescription(ExternalSystemBeforeRunTask task) {
    final String externalProjectPath = task.getTaskExecutionSettings().getExternalProjectPath();

    if (task.getTaskExecutionSettings().getTaskNames().isEmpty()) {
      return ExternalSystemBundle.message("tasks.before.run.empty", mySystemId.getReadableName());
    }

    String desc = StringUtil.join(task.getTaskExecutionSettings().getTaskNames(), " ");
    for (Module module : ModuleManager.getInstance(myProject).getModules()) {
      if (!ExternalSystemApiUtil.isExternalSystemAwareModule(mySystemId, module)) continue;

      if (StringUtil.equals(externalProjectPath, ExternalSystemApiUtil.getExternalProjectPath(module))) {
        desc = module.getName() + ": " + desc;
        break;
      }
    }

    return ExternalSystemBundle.message("tasks.before.run", mySystemId.getReadableName(), desc);
  }
}
