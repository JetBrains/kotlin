/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.ui;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.ModuleNode;
import com.intellij.openapi.externalSystem.view.ProjectNode;
import com.intellij.openapi.externalSystem.view.TaskNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.treeStructure.NullNode;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.InputEvent;

/**
 * @author Vladislav.Soroka
 */
public class SelectExternalTaskDialog extends SelectExternalSystemNodeDialog {

  private static final Class<? extends ExternalSystemNode>[] NODE_CLASSES = ContainerUtil.ar(
    ProjectNode.class,
    ModuleNode.class,
    TaskNode.class
  );
  private Pair<String, TaskData> myResult;

  public SelectExternalTaskDialog(@NotNull ProjectSystemId systemId, Project project) {
    super(systemId, project, String.format("Choose %s Task", systemId.getReadableName()), NODE_CLASSES, null);
    groupTasks = Boolean.FALSE;
    useTasksNode = Boolean.FALSE;
    init();
  }

  @Override
  protected void doOKAction() {
    SimpleNode node = getSelectedNode();
    if (node instanceof NullNode) node = null;

    myResult = node instanceof TaskNode ? Pair.create(((TaskNode)node).getModuleOwnerName(), ((TaskNode)node).getData()) : null;
    super.doOKAction();
  }

  @Override
  public void doCancelAction() {
    super.doCancelAction();
    myResult = null;
  }

  @Override
  protected void handleDoubleClickOrEnter(@NotNull ExternalSystemNode node, @Nullable String actionId, InputEvent inputEvent) {
    if (node instanceof ProjectNode) {
      doOKAction();
    }
  }

  public Pair<String, TaskData> getResult() {
    return myResult;
  }

  @Override
  protected Object customizeProjectsTreeRoot(Object rootElement) {
    if (rootElement instanceof ExternalSystemNode && ((ExternalSystemNode)rootElement).getChildCount() == 1) {
      ExternalSystemNode node = ((ExternalSystemNode)rootElement).getChildren()[0];
      return (node instanceof ProjectNode) && ((ProjectNode)node).isSingleModuleProject() ? rootElement : node;
    }
    else {
      return rootElement;
    }
  }
}
