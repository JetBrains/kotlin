// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.task.ui;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.view.ExternalProjectsViewImpl;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.impl.ContentImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Denis Zhdanov
 */
public abstract class AbstractExternalSystemToolWindowFactory implements ToolWindowFactory, DumbAware {

  @NotNull private final ProjectSystemId myExternalSystemId;

  protected AbstractExternalSystemToolWindowFactory(@NotNull ProjectSystemId id) {
    myExternalSystemId = id;
  }

  @Override
  public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
    toolWindow.setTitle(myExternalSystemId.getReadableName());
    ContentManager contentManager = toolWindow.getContentManager();

    contentManager.addContent(new ContentImpl(createInitializingLabel(), "", false));

    ExternalProjectsManager.getInstance(project).runWhenInitialized(() -> {
        final ExternalProjectsViewImpl projectsView = new ExternalProjectsViewImpl(project, (ToolWindowEx)toolWindow, myExternalSystemId);
        ExternalProjectsManagerImpl.getInstance(project).registerView(projectsView);
        ContentImpl tasksContent = new ContentImpl(projectsView, "", true);
        contentManager.removeAllContents(true);
        contentManager.addContent(tasksContent);
      });
  }

  @NotNull
  private JLabel createInitializingLabel() {
    JLabel label = new JLabel("Initializing " + myExternalSystemId.getReadableName() + " projects data...", SwingConstants.CENTER);
    label.setOpaque(true);
    return label;
  }
}
