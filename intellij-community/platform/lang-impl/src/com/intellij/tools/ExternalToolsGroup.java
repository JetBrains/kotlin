// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.tools;

import java.util.List;

public class ExternalToolsGroup extends BaseExternalToolsGroup<Tool> {
  public ExternalToolsGroup() {
    super(ToolManager.getInstance().getGroups());
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
