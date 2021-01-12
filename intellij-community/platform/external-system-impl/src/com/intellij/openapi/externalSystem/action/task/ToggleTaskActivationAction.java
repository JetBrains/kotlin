// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action.task;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.action.ExternalSystemToggleAction;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalSystemTaskActivator;
import com.intellij.openapi.externalSystem.statistics.ExternalSystemActionsCollector;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.RunConfigurationNode;
import com.intellij.openapi.externalSystem.view.TaskNode;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static com.intellij.openapi.externalSystem.service.project.manage.ExternalSystemTaskActivator.RUN_CONFIGURATION_TASK_PREFIX;

/**
 * @author Vladislav.Soroka
 */
public abstract class ToggleTaskActivationAction extends ExternalSystemToggleAction {

  @NotNull
  private final ExternalSystemTaskActivator.Phase myPhase;

  protected ToggleTaskActivationAction(@NotNull ExternalSystemTaskActivator.Phase phase) {
    myPhase = phase;
  }

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    return super.isEnabled(e) && !getTasks(e).isEmpty();
  }

  @Override
  protected boolean doIsSelected(@NotNull AnActionEvent e) {
    return hasTask(getTaskActivator(e), getTasks(e).get(0));
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    ExternalSystemActionsCollector.trigger(getProject(e), getSystemId(e), this, e);
    List<TaskData> tasks = getTasks(e);
    if (state) {
      addTasks(getTaskActivator(e), tasks);
    }
    else {
      removeTasks(getTaskActivator(e), tasks);
    }
  }

  @NotNull
  private static List<TaskData> getTasks(@NotNull AnActionEvent e) {
    final List<ExternalSystemNode> selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES);
    if (selectedNodes == null) return Collections.emptyList();

    List<TaskData> tasks = new SmartList<>();
    for (ExternalSystemNode node : selectedNodes) {
      if (node instanceof TaskNode && !node.isIgnored()) {
        tasks.add((TaskData)node.getData());
      }
      else if (node instanceof RunConfigurationNode) {
        final RunnerAndConfigurationSettings configurationSettings = ((RunConfigurationNode)node).getSettings();
        final ExternalSystemRunConfiguration runConfiguration = (ExternalSystemRunConfiguration)configurationSettings.getConfiguration();
        final ExternalSystemTaskExecutionSettings taskExecutionSettings = runConfiguration.getSettings();
        tasks.add(new TaskData(taskExecutionSettings.getExternalSystemId(), RUN_CONFIGURATION_TASK_PREFIX + configurationSettings.getName(),
                               taskExecutionSettings.getExternalProjectPath(), null));
      }
      else {
        return Collections.emptyList();
      }
    }
    return tasks;
  }

  protected boolean hasTask(ExternalSystemTaskActivator manager, TaskData taskData) {
    if (taskData == null) return false;
    return manager.isTaskOfPhase(taskData, myPhase);
  }

  private void addTasks(ExternalSystemTaskActivator taskActivator, List<TaskData> tasks) {
    taskActivator.addTasks(tasks, myPhase);
  }

  private void removeTasks(ExternalSystemTaskActivator taskActivator, List<TaskData> tasks) {
    taskActivator.removeTasks(tasks, myPhase);
  }


  private ExternalSystemTaskActivator getTaskActivator(@NotNull AnActionEvent e) {
    return ExternalProjectsManagerImpl.getInstance(getProject(e)).getTaskActivator();
  }
}
