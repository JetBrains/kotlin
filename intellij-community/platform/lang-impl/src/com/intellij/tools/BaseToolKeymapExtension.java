// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.keymap.KeymapExtension;
import com.intellij.openapi.keymap.KeymapGroup;
import com.intellij.openapi.keymap.impl.ui.Group;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;

import java.util.List;

public abstract class BaseToolKeymapExtension implements KeymapExtension {

  @Override
  public KeymapGroup createGroup(final Condition<AnAction> filtered, final Project project) {
    final ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
    Group rootGroup = new Group(getRootGroupName(), getRootGroupId(), AllIcons.Nodes.KeymapTools);
    List<ToolsGroup<Tool>> groups = ToolManager.getInstance().getGroups();

    for (ToolsGroup<Tool> toolsGroup : groups) {
      String groupName = toolsGroup.getName();
      Group group = new Group(groupName, getGroupIdPrefix() + groupName, null);
      List<? extends Tool> tools = getToolsIdsByGroupName(groupName);
      for (Tool tool : tools) {
        if (filtered != null && !filtered.value(actionManager.getActionOrStub(tool.getActionId()))) continue;
        group.addGroup(new Group(tool.getName(), tool.getActionId(), null));
      }
      rootGroup.addGroup(group);
    }
    return rootGroup;
  }

  protected abstract String getGroupIdPrefix();

  protected abstract String getActionIdPrefix();

  protected abstract List<? extends Tool> getToolsIdsByGroupName(String groupName);

  protected abstract String getRootGroupName();

  protected abstract String getRootGroupId();
}
