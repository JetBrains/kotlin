// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author traff
 */
public abstract class AbstractToolBeforeRunTaskProvider<T extends AbstractToolBeforeRunTask> extends BeforeRunTaskProvider<T> {
  protected static final Logger LOG = Logger.getInstance(ToolBeforeRunTaskProvider.class);

  @Override
  public Icon getIcon() {
    return AllIcons.General.ExternalTools;
  }

  @Override
  public boolean configureTask(@NotNull RunConfiguration runConfiguration, @NotNull T task) {
    final ToolSelectDialog dialog = new ToolSelectDialog(runConfiguration.getProject(), task.getToolActionId(), createToolsPanel());
    if (!dialog.showAndGet()) {
      return false;
    }
    boolean isModified = dialog.isModified();
    Tool selectedTool = dialog.getSelectedTool();
    if (selectedTool == null) {
      return true;
    }
    String selectedToolId = selectedTool.getActionId();
    String oldToolId = task.getToolActionId();
    if (oldToolId != null && oldToolId.equals(selectedToolId)) {
      return isModified;
    }
    task.setToolActionId(selectedToolId);
    return true;
  }

  protected abstract BaseToolsPanel createToolsPanel();

  @Override
  public boolean canExecuteTask(@NotNull RunConfiguration configuration, @NotNull T task) {
    return task.isExecutable();
  }

  @Override
  public String getDescription(T task) {
    final String actionId = task.getToolActionId();
    if (actionId == null) {
      LOG.error("Null id");
      return ToolsBundle.message("tools.unknown.external.tool");
    }
    Tool tool = task.findCorrespondingTool();
    if (tool == null) {
      return ToolsBundle.message("tools.unknown.external.tool");
    }
    String groupName = tool.getGroup();
    return ToolsBundle
      .message("tools.before.run.description", StringUtil.isEmpty(groupName) ? tool.getName() : groupName + "/" + tool.getName()) + (!tool.isEnabled() ? " (disabled)" : "");
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  public boolean executeTask(@NotNull DataContext context, @NotNull RunConfiguration configuration, @NotNull ExecutionEnvironment env, @NotNull T task) {
    if (!task.isExecutable()) {
      return false;
    }
    return task.execute(context, env.getExecutionId());
  }
}
