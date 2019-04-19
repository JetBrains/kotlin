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
import com.intellij.execution.RunManager;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.content.Content;
import com.intellij.util.containers.JBIterable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.intellij.execution.dashboard.actions.RunDashboardActionUtils.getTargets;

/**
 * @author konstantin.aleev
 */
public class RemoveConfigurationAction extends AnAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null || !RunDashboardManager.getInstance(project).isShowConfigurations()) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    JBIterable<RunDashboardRunConfigurationNode> targets = getTargets(e);
    RunManager runManager = RunManager.getInstance(project);
    boolean enabled = targets.isNotEmpty() && targets.filter(node -> !runManager.hasSettings(node.getConfigurationSettings())).isEmpty();

    if (enabled) {
      RunDashboardRunConfigurationNode node = targets.single();
      Content content = node == null ? null : node.getContent();
      JComponent contentComponent = content == null ? null : content.getComponent();
      if (contentComponent != null && ActionPlaces.MAIN_MENU.equals(e.getPlace()) &&
          UIUtil.isAncestor(content.getComponent(), e.getData(PlatformDataKeys.CONTEXT_COMPONENT))) {
        e.getPresentation().setEnabledAndVisible(false);
        return;
      }
    }

    e.getPresentation().setEnabled(enabled);
    e.getPresentation().setVisible(targets.isNotEmpty());
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    JBIterable<RunDashboardRunConfigurationNode> nodes = getTargets(e);
    if (nodes.isEmpty()) return;

    int size = nodes.size();
    if (Messages.showYesNoDialog(project,
                                 "Delete " + size + " " + StringUtil.pluralize("configuration", size) + "?",
                                 ExecutionBundle.message("run.dashboard.remove.configuration.dialog.title"),
                                 Messages.getWarningIcon())
        != Messages.YES) {
      return;
    }

    RunManager runManager = RunManager.getInstance(project);
    for (RunDashboardRunConfigurationNode node : nodes) {
      runManager.removeConfiguration(node.getConfigurationSettings());
    }
  }
}
