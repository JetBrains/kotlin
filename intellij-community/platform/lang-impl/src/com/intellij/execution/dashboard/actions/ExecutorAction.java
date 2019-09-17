// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.*;
import com.intellij.execution.compound.CompoundRunConfiguration;
import com.intellij.execution.compound.SettingsAndEffectiveTarget;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManagerImpl;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

import static com.intellij.execution.dashboard.actions.RunDashboardActionUtils.getLeafTargets;

/**
 * @author konstantin.aleev
 */
public abstract class ExecutorAction extends DumbAwareAction {
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
    boolean running = targetNodes.filter(node -> {
      Content content = node.getContent();
      return content != null && !RunContentManagerImpl.isTerminated(content);
    }).isNotEmpty();
    update(e, running);
    e.getPresentation().setEnabled(targetNodes.filter(this::canRun).isNotEmpty());
  }

  private boolean canRun(@NotNull RunDashboardRunConfigurationNode node) {
    Project project = node.getProject();
    return canRun(node.getConfigurationSettings(),
                  ExecutionTargetManager.getActiveTarget(project),
                  DumbService.isDumb(project));
  }

  private boolean canRun(RunnerAndConfigurationSettings settings, ExecutionTarget target, boolean isDumb) {
    if (isDumb && !settings.getType().isDumbAware()) return false;

    String executorId = getExecutor().getId();
    RunConfiguration configuration = settings.getConfiguration();
    Project project = configuration.getProject();
    if (configuration instanceof CompoundRunConfiguration) {
      if (ExecutionTargetManager.getInstance(project).getTargetsFor(configuration).isEmpty()) return false;

      List<SettingsAndEffectiveTarget> subConfigurations =
        ((CompoundRunConfiguration)configuration).getConfigurationsWithEffectiveRunTargets();
      if (subConfigurations.isEmpty()) return false;

      RunManager runManager = RunManager.getInstance(project);
      for (SettingsAndEffectiveTarget subConfiguration : subConfigurations) {
        RunnerAndConfigurationSettings subSettings = runManager.findSettings(subConfiguration.getConfiguration());
        if (subSettings == null || !canRun(subSettings, subConfiguration.getTarget(), isDumb)) {
          return false;
        }
      }
      return true;
    }

    if (!isValid(settings)) return false;

    ProgramRunner<?> runner = ProgramRunner.getRunner(executorId, configuration);
    return runner != null && ExecutionTargetManager.canRun(configuration, target) &&
          !ExecutorRegistry.getInstance().isStarting(project, executorId, runner.getRunnerId());
  }

  private static boolean isValid(RunnerAndConfigurationSettings settings) {
    try {
      settings.checkSettings(null);
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

    for (RunDashboardRunConfigurationNode node : getLeafTargets(e)) {
      doActionPerformed(node);
    }
  }

  private void doActionPerformed(RunDashboardRunConfigurationNode node) {
    if (!canRun(node)) return;

    run(node.getConfigurationSettings(), ExecutionTargetManager.getActiveTarget(node.getProject()), node.getDescriptor());
  }

  private void run(RunnerAndConfigurationSettings settings, ExecutionTarget target, RunContentDescriptor descriptor) {
    RunConfiguration configuration = settings.getConfiguration();
    Project project = configuration.getProject();
    if (configuration instanceof CompoundRunConfiguration) {
      RunManager runManager = RunManager.getInstance(project);
      List<SettingsAndEffectiveTarget> subConfigurations =
        ((CompoundRunConfiguration)configuration).getConfigurationsWithEffectiveRunTargets();
      for (SettingsAndEffectiveTarget subConfiguration : subConfigurations) {
        RunnerAndConfigurationSettings subSettings = runManager.findSettings(subConfiguration.getConfiguration());
        if (subSettings != null) {
          run(subSettings, subConfiguration.getTarget(), null);
        }
      }
    }
    else {
      ProcessHandler processHandler = descriptor == null ? null : descriptor.getProcessHandler();
      ExecutionManager.getInstance(project).restartRunProfile(project, getExecutor(), target, settings, processHandler);
    }
  }

  protected abstract Executor getExecutor();

  protected abstract void update(@NotNull AnActionEvent e, boolean running);
}
