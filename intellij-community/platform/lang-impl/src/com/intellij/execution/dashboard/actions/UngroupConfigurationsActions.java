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

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.dashboard.RunDashboardManager.RunDashboardService;
import com.intellij.execution.dashboard.tree.FolderDashboardGroupingRule;
import com.intellij.execution.dashboard.tree.GroupingNode;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.execution.services.ServiceViewActionUtils.getTargets;

/**
 * @author Konstantin Aleev
 */
public class UngroupConfigurationsActions extends AnAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    JBIterable<GroupingNode> targets = getTargets(e, GroupingNode.class);
    boolean enabled = targets.isNotEmpty() &&
                      targets.filter(node -> !(node.getGroup() instanceof FolderDashboardGroupingRule.FolderDashboardGroup)).isEmpty();
    e.getPresentation().setEnabledAndVisible(enabled);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    for (GroupingNode node : getTargets(e, GroupingNode.class)) {
      doActionPerformed(project, node);
    }
  }

  private static void doActionPerformed(Project project, GroupingNode node) {
    String groupName = node.getGroup().getName();
    List<RunDashboardService> services =
      RunDashboardManager.getInstance(project).getRunConfigurations();

    final RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);
    runManager.fireBeginUpdate();
    try {
      for (RunDashboardService service : services) {
        RunnerAndConfigurationSettings settings = service.getSettings();
        if (groupName.equals(settings.getFolderName())) {
          settings.setFolderName(null);
        }
      }
    }
    finally {
      runManager.fireEndUpdate();
    }
  }
}
