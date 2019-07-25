// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Eugene Belyaev
 */
public abstract class BaseExternalToolsGroup<T extends Tool> extends SimpleActionGroup implements DumbAware {
  @Override
  public void update(@NotNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    removeAll();
    Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }
    presentation.setEnabled(true);
    List<ToolsGroup<T>> groups = getToolsGroups();
    for (ToolsGroup group : groups) {
      String groupName = group.getName();
      if (!StringUtil.isEmptyOrSpaces(groupName)) {
        SimpleActionGroup subgroup = new SimpleActionGroup();
        subgroup.getTemplatePresentation().setText(groupName, false);
        subgroup.setPopup(true);
        fillGroup(groupName, subgroup);
        if (subgroup.getChildrenCount() > 0) {
          add(subgroup);
        }
      }
      else {
        fillGroup(null, this);
      }
    }
    presentation.setVisible(getChildrenCount() > 0);
  }

  protected abstract List<ToolsGroup<T>> getToolsGroups();

  private void fillGroup(@Nullable String groupName, SimpleActionGroup group) {
    List<T> tools = getToolsByGroupName(groupName);
    for (T tool : tools) {
      // We used to have a bunch of IFs checking whether we want to show the given tool in the given event.getPlace().
      // But now from the UX point of view we believe we'd better remove a bunch of checkboxes from the Edit External Tool dialog.
      // See IDEA-190856 for discussion.
      if (tool.isEnabled()) {
        addToolToGroup(tool, group);
      }
    }
  }

  protected abstract List<T> getToolsByGroupName(String groupName);

  private void addToolToGroup(T tool, SimpleActionGroup group) {
    String id = tool.getActionId();
    AnAction action = ActionManager.getInstance().getAction(id);
    if (action == null) {
      action = createToolAction(tool);
    }

    group.add(action);
  }

  protected abstract ToolAction createToolAction(T tool);
}
