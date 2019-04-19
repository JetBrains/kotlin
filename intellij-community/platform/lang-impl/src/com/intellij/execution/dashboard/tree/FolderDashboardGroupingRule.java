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

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.dashboard.RunDashboardGroup;
import com.intellij.execution.dashboard.RunDashboardGroupingRule;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.smartTree.ActionPresentation;
import com.intellij.ide.util.treeView.smartTree.ActionPresentationData;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author konstantin.aleev
 */
public class FolderDashboardGroupingRule implements RunDashboardGroupingRule {
  @NonNls private static final String NAME = "FolderDashboardGroupingRule";

  @Override
  @NotNull
  public String getName() {
    return NAME;
  }

  @NotNull
  @Override
  public ActionPresentation getPresentation() {
    return new ActionPresentationData("", "", null);
  }

  @Override
  public boolean isAlwaysEnabled() {
    return true;
  }

  @Override
  public boolean shouldGroupSingleNodes() {
    return true;
  }

  @Nullable
  @Override
  public RunDashboardGroup getGroup(AbstractTreeNode<?> node) {
    if (node instanceof RunDashboardRunConfigurationNode) {
      RunnerAndConfigurationSettings configurationSettings = ((RunDashboardRunConfigurationNode)node).getConfigurationSettings();
      String folderName = configurationSettings.getFolderName();
      if (folderName != null) {
        return new FolderDashboardGroup(folderName, folderName, AllIcons.Nodes.Folder);
      }
    }
    return null;
  }

  public static class FolderDashboardGroup extends RunDashboardGroupImpl<String> {
    public FolderDashboardGroup(String value, String name, Icon icon) {
      super(value, name, icon);
    }
  }
}
