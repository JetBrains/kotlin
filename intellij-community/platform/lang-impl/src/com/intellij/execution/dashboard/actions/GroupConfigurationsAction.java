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
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import static com.intellij.execution.dashboard.actions.RunDashboardActionUtils.getTargets;

/**
 * @author Konstantin Aleev
 */
public class GroupConfigurationsAction extends AnAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(getTargets(e).isNotEmpty());
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      e.getPresentation().setText(getTemplatePresentation().getText() + "...");
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    JBIterable<RunDashboardRunConfigurationNode> nodes = getTargets(e);
    RunDashboardRunConfigurationNode firstNode = nodes.first();
    String initialValue = firstNode != null ? firstNode.getConfigurationSettings().getFolderName() : null;
    String value = Messages.showInputDialog(project, ExecutionBundle.message("run.dashboard.group.configurations.label"),
                                            ExecutionBundle.message("run.dashboard.group.configurations.title"), null, initialValue, null);
    if (value == null) return;

    String groupName = value.isEmpty() ? null : value; // If input value is empty then ungroup nodes.

    final RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
    runManager.fireBeginUpdate();
    try {
      nodes.forEach(node -> node.getConfigurationSettings().setFolderName(groupName));
    }
    finally {
      runManager.fireEndUpdate();
    }
  }
}
