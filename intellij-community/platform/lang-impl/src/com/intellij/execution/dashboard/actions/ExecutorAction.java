// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.*;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManagerImpl;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.intellij.execution.dashboard.actions.RunDashboardActionUtils.getLeafTargets;

/**
 * @author konstantin.aleev
 */
public abstract class ExecutorAction extends AnAction {
  protected ExecutorAction() {
  }

  protected ExecutorAction(String text, String description, Icon icon) {
    super(text, description, icon);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      update(e, false);
      return;
    }
    JBIterable<RunDashboardRunConfigurationNode> targetNodes = getLeafTargets(e);
    if (RunDashboardManager.getInstance(project).isShowConfigurations()) {
      boolean running = targetNodes.filter(node -> {
        Content content = node.getContent();
        return content != null && !RunContentManagerImpl.isTerminated(content);
      }).isNotEmpty();
      update(e, running);
      e.getPresentation().setEnabled(targetNodes.filter(this::canRun).isNotEmpty());
    }
    else {
      Content content = RunDashboardManager.getInstance(project).getDashboardContentManager().getSelectedContent();
      update(e, content != null && !RunContentManagerImpl.isTerminated(content));
      e.getPresentation().setEnabled(content != null);
    }
  }

  private boolean canRun(@NotNull RunDashboardRunConfigurationNode node) {
    final String executorId = getExecutor().getId();
    final RunnerAndConfigurationSettings configurationSettings = node.getConfigurationSettings();
    final ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, configurationSettings);
    final ExecutionTarget target = ExecutionTargetManager.getActiveTarget(node.getProject());

    RunConfiguration configuration = configurationSettings.getConfiguration();
    return isValid(node) &&
           runner != null &&
           runner.canRun(executorId, configuration) &&
           ExecutionTargetManager.canRun(configuration, target) &&
           !isStarting(node.getProject(), configurationSettings, executorId, runner.getRunnerId());
  }

  private static boolean isStarting(Project project, RunnerAndConfigurationSettings configurationSettings, String executorId, String runnerId) {
    ExecutorRegistry executorRegistry = ExecutorRegistry.getInstance();
    if (executorRegistry.isStarting(project, executorId, runnerId)) {
      return true;
    }

    if (configurationSettings.getConfiguration().isAllowRunningInParallel()) {
      return false;
    }

    for (Executor executor : executorRegistry.getRegisteredExecutors()) {
      if (executor.getId().equals(executorId)) continue;

      ProgramRunner runner = ProgramRunnerUtil.getRunner(executor.getId(), configurationSettings);
      if (runner == null) continue;

      if (executorRegistry.isStarting(project, executor.getId(), runner.getRunnerId())) return true;
    }
    return false;
  }

  private boolean isValid(RunDashboardRunConfigurationNode node) {
    try {
      node.getConfigurationSettings().checkSettings(getExecutor());
      return true;
    }
    catch (IndexNotReadyException ex) {
      return true;
    }
    catch (RuntimeConfigurationError ex) {
      return false;
    }
    catch (RuntimeConfigurationException ex) {
      return true;
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    if (RunDashboardManager.getInstance(project).isShowConfigurations()) {
      for (RunDashboardRunConfigurationNode node : getLeafTargets(e)) {
        doActionPerformed(node);
      }
    }
    else {
      Content content = RunDashboardManager.getInstance(project).getDashboardContentManager().getSelectedContent();
      if (content != null) {
        RunContentDescriptor descriptor = RunContentManagerImpl.getRunContentDescriptorByContent(content);
        JComponent component = content.getComponent();
        if (component == null) {
          return;
        }
        ExecutionEnvironment environment = LangDataKeys.EXECUTION_ENVIRONMENT.getData(DataManager.getInstance().getDataContext(component));
        if (environment == null) {
          return;
        }
        ExecutionManager.getInstance(project).restartRunProfile(project,
                                                                getExecutor(),
                                                                ExecutionTargetManager.getActiveTarget(project),
                                                                environment.getRunnerAndConfigurationSettings(),
                                                                descriptor == null ? null : descriptor.getProcessHandler());
      }
    }
  }

  private void doActionPerformed(RunDashboardRunConfigurationNode node) {
    if (!canRun(node)) return;

    RunContentDescriptor descriptor = node.getDescriptor();
    ExecutionManager.getInstance(node.getProject()).restartRunProfile(node.getProject(),
                                                                      getExecutor(),
                                                                      ExecutionTargetManager.getActiveTarget(node.getProject()),
                                                                      node.getConfigurationSettings(),
                                                                      descriptor == null ? null : descriptor.getProcessHandler());
  }

  protected abstract Executor getExecutor();

  protected abstract void update(@NotNull AnActionEvent e, boolean running);
}
