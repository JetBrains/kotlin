// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action;

import com.intellij.execution.*;
import com.intellij.execution.executors.ExecutorGroup;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.statistics.ExternalSystemActionsCollector;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.RunConfigurationNode;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class ExternalSystemRunConfigurationMenu extends DefaultActionGroup implements DumbAware {
  @Override
  public void update(@NotNull AnActionEvent e) {
    for (AnAction action : getChildActionsOrStubs()) {
      if (action instanceof ExecuteExternalSystemRunConfigurationAction) {
        remove(action);
      }
    }

    final Project project = e.getProject();

    final List<ExternalSystemNode> selectedNodes = e.getData(ExternalSystemDataKeys.SELECTED_NODES);
    if (selectedNodes == null || selectedNodes.size() != 1 || !(selectedNodes.get(0) instanceof RunConfigurationNode)) return;

    RunConfigurationNode runConfigurationNode = (RunConfigurationNode)selectedNodes.get(0);
    final RunnerAndConfigurationSettings settings = runConfigurationNode.getSettings();

    if (settings == null || project == null) return;

    ProjectSystemId projectSystemId = e.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
    @SuppressWarnings("DuplicatedCode") final List<Executor> executors = new ArrayList<>();
    for (final Executor executor: ExecutorRegistry.getInstance().getRegisteredExecutors()) {
      if (executor instanceof ExecutorGroup) {
        executors.addAll(((ExecutorGroup<?>)executor).childExecutors());
      }
      else {
        executors.add(executor);
      }
    }
    for (int i = executors.size(); --i >= 0; ) {
      Executor executor = executors.get(i);
      if(!executor.isApplicable(project)) continue;
      final ProgramRunner runner = ProgramRunner.getRunner(executor.getId(), settings.getConfiguration());
      AnAction action = new ExecuteExternalSystemRunConfigurationAction(executor, runner != null, project, projectSystemId, settings);
      addAction(action, Constraints.FIRST);
    }

    super.update(e);
  }

  private static class ExecuteExternalSystemRunConfigurationAction extends AnAction {
    private final Executor myExecutor;
    private final boolean myEnabled;
    private final Project myProject;
    private final RunnerAndConfigurationSettings mySettings;
    private final ProjectSystemId mySystemId;

    ExecuteExternalSystemRunConfigurationAction(Executor executor,
                                                boolean enabled,
                                                Project project,
                                                ProjectSystemId projectSystemId,
                                                RunnerAndConfigurationSettings settings) {
      super(executor.getActionName(), null, executor.getIcon());
      myExecutor = executor;
      myEnabled = enabled;
      myProject = project;
      mySettings = settings;
      mySystemId = projectSystemId;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
      if (myEnabled) {
        ExternalSystemActionsCollector.trigger(myProject, mySystemId, this, event);
        ProgramRunnerUtil.executeConfiguration(mySettings, myExecutor);
        RunManager.getInstance(myProject).setSelectedConfiguration(mySettings);
      }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(myEnabled);
    }
  }
}
