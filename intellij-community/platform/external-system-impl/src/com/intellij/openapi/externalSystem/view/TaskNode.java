/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.view;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.Named;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.openapi.externalSystem.model.ProjectKeys.MODULE;
import static com.intellij.openapi.externalSystem.model.ProjectKeys.PROJECT;

/**
 * @author Vladislav.Soroka
 */
public class TaskNode extends ExternalSystemNode<TaskData> {
  private final TaskData myTaskData;
  private String moduleOwnerName;

  public TaskNode(@NotNull ExternalProjectsView externalProjectsView, @NotNull DataNode<TaskData> dataNode) {
    super(externalProjectsView, null, dataNode);
    myTaskData = dataNode.getData();

    DataNode parent = ExternalSystemApiUtil.findParent(dataNode, MODULE);
    if (parent == null) {
      parent = ExternalSystemApiUtil.findParent(dataNode, PROJECT);
    }
    if(parent != null && parent.getData() instanceof Named) {
      moduleOwnerName = ((Named)parent.getData()).getInternalName();
    }
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    super.update(presentation);
    presentation.setIcon(getUiAware().getTaskIcon());

    final String shortcutHint = StringUtil.nullize(getShortcutsManager().getDescription(myTaskData.getLinkedExternalProjectPath(), myTaskData.getName()));
    final String activatorHint = StringUtil.nullize(getTaskActivator().getDescription(
      myTaskData.getOwner(), myTaskData.getLinkedExternalProjectPath(), myTaskData.getName()));

    String hint;
    if (shortcutHint == null) {
      hint = activatorHint;
    }
    else if (activatorHint == null) {
      hint = shortcutHint;
    }
    else {
      hint = shortcutHint + ", " + activatorHint;
    }

    setNameAndTooltip(getName(), myTaskData.getDescription(), hint);
  }

  @Override
  public boolean isVisible() {
    if (!super.isVisible()) return false;
    return !myTaskData.isInherited() || getExternalProjectsView().showInheritedTasks();
  }

  public boolean isTest() {
    return myTaskData.isTest();
  }

  public String getModuleOwnerName() {
    return moduleOwnerName;
  }

  @Nullable
  @Override
  protected String getMenuId() {
    return "ExternalSystemView.TaskMenu";
  }

  @Nullable
  @Override
  protected String getActionId() {
    return "ExternalSystem.RunTask";
  }

  @Override
  public boolean isAlwaysLeaf() {
    return true;
  }
}
