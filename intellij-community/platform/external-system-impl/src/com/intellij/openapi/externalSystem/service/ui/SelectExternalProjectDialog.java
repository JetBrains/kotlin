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
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.externalSystem.view.ProjectNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.NullNode;
import com.intellij.ui.treeStructure.SimpleNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;

/**
 * @author Vladislav.Soroka
 */
public class SelectExternalProjectDialog extends SelectExternalSystemNodeDialog {

  private ProjectData myResult;

  public SelectExternalProjectDialog(@NotNull ProjectSystemId systemId, Project project, final ProjectData current) {
    super(systemId, project, String.format("Select %s Project", systemId.getReadableName()), ProjectNode.class,
          node -> node instanceof ProjectNode && ((ProjectNode)node).getData() == current);
    assert current == null || current.getOwner().equals(systemId);
    init();
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    Action selectNoneAction = new AbstractAction("&None") {
      @Override
      public void actionPerformed(ActionEvent e) {
        doOKAction();
        myResult = null;
      }
    };
    return new Action[]{selectNoneAction, getOKAction(), getCancelAction()};
  }

  @Override
  protected void doOKAction() {
    SimpleNode node = getSelectedNode();
    if (node instanceof NullNode) node = null;

    myResult = node instanceof ProjectNode ? ((ProjectNode)node).getData() : null;
    super.doOKAction();
  }

  @Override
  protected void handleDoubleClickOrEnter(@NotNull ExternalSystemNode node, @Nullable String actionId, InputEvent inputEvent) {
    if(node instanceof ProjectNode ) {
      doOKAction();
    }
  }

  public ProjectData getResult() {
    return myResult;
  }
}
