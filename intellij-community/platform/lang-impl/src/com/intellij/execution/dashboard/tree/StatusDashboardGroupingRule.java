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
package com.intellij.execution.dashboard.tree;

import com.intellij.execution.dashboard.RunDashboardGroup;
import com.intellij.execution.dashboard.RunDashboardGroupingRule;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationStatus;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author konstantin.aleev
 */
public class StatusDashboardGroupingRule implements RunDashboardGroupingRule {
  @NonNls public static final String NAME = "StatusDashboardGroupingRule";

  @Override
  @NotNull
  public String getName() {
    return NAME;
  }

  @Nullable
  @Override
  public RunDashboardGroup getGroup(AbstractTreeNode<?> node) {
    Project project = node.getProject();
    if (project != null && !PropertiesComponent.getInstance(project).getBoolean(getName(), true)) {
      return null;
    }
    if (node instanceof RunDashboardRunConfigurationNode) {
      RunDashboardRunConfigurationNode runConfigurationNode = (RunDashboardRunConfigurationNode)node;
      RunDashboardRunConfigurationStatus status = runConfigurationNode.getStatus();
      return new RunDashboardGroupImpl<>(status, status.getName(), status.getIcon());
    }
    return null;
  }
}
