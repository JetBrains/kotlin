/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.packageDependencies;

import com.intellij.icons.AllIcons;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class DependenciesToolWindow {
  private final Project myProject;
  private ContentManager myContentManager;

  public static DependenciesToolWindow getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, DependenciesToolWindow.class);
  }

  public DependenciesToolWindow(@NotNull Project project) {
    myProject = project;
    StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> {
      final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
      if (toolWindowManager == null) {
        return;
      }

      ToolWindow toolWindow = toolWindowManager.registerToolWindow(ToolWindowId.DEPENDENCIES,
                                                                   true,
                                                                   ToolWindowAnchor.BOTTOM,
                                                                   project);
      toolWindow.setHelpId("dependency.viewer.tool.window");
      toolWindow.getComponent().putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true");
      myContentManager = toolWindow.getContentManager();

      toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowInspection);
      new ContentManagerWatcher(toolWindow, myContentManager);
    });
  }

  public void addContent(@NotNull Content content) {
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> {
      myContentManager.addContent(content);
      myContentManager.setSelectedContent(content);
      ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.DEPENDENCIES).activate(null);
    });
  }

  public void closeContent(Content content) {
    myContentManager.removeContent(content, true);
  }
}
