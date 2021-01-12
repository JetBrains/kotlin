// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.tools;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ExternalToolsGroup extends BaseExternalToolsGroup<Tool> {
  public static final String GROUP_ID_PREFIX = "ExternalTools_";

  @Override
  protected List<ToolsGroup<Tool>> getToolsGroups() {
    return ToolManager.getInstance().getGroups();
  }

  @Override
  protected @NotNull String getGroupIdPrefix() {
    return GROUP_ID_PREFIX;
  }

  @Override
  protected List<Tool> getToolsByGroupName(String groupName) {
    return ToolManager.getInstance().getTools(groupName);
  }

  @Override
  protected ToolAction createToolAction(Tool tool) {
    return new ToolAction(tool);
  }
}
