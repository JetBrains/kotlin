// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action.task;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.action.ExternalSystemNodeAction;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalSystemKeymapExtension;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalSystemShortcutsManager;
import com.intellij.openapi.externalSystem.statistics.ExternalSystemActionsCollector;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.ModuleNode;
import com.intellij.openapi.externalSystem.view.ProjectNode;
import com.intellij.openapi.keymap.impl.ui.EditKeymapsDialog;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class AssignShortcutAction extends ExternalSystemNodeAction<TaskData> {

  public AssignShortcutAction() {
    super(TaskData.class);
  }

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    return super.isEnabled(e) && !isIgnoredNode(e);
  }

  @Override
  protected void perform(@NotNull Project project,
                         @NotNull ProjectSystemId projectSystemId,
                         @NotNull TaskData taskData,
                         @NotNull AnActionEvent e) {
    ExternalSystemActionsCollector.trigger(project, projectSystemId, this, e);
    final ExternalSystemShortcutsManager shortcutsManager = ExternalProjectsManagerImpl.getInstance(project).getShortcutsManager();
    final String actionId = shortcutsManager.getActionId(taskData.getLinkedExternalProjectPath(), taskData.getName());
    AnAction action = ActionManager.getInstance().getAction(actionId);
    if (action == null) {
      ExternalSystemNode<?> taskNode = ContainerUtil.getFirstItem(e.getData(ExternalSystemDataKeys.SELECTED_NODES));
      assert taskNode != null;
      final String group;
      final ModuleNode moduleDataNode = taskNode.findParent(ModuleNode.class);
      if (moduleDataNode != null) {
        ModuleData moduleData = moduleDataNode.getData();
        group = moduleData != null ? moduleData.getInternalName() : null;
      }
      else {
        ProjectNode projectNode = taskNode.findParent(ProjectNode.class);
        ProjectData projectData = projectNode != null ? projectNode.getData() : null;
        group = projectData != null ? projectData.getInternalName() : null;
      }
      if (group != null) {
        ExternalSystemKeymapExtension.getOrRegisterAction(project, group, taskData);
      }
    }
    new EditKeymapsDialog(project, actionId).show();
  }
}
