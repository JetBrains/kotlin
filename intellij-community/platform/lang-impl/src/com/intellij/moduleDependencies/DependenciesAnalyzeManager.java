// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.moduleDependencies;

import com.intellij.icons.AllIcons;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author anna
 */
@State(name = "DependenciesAnalyzeManager", storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
public class DependenciesAnalyzeManager implements PersistentStateComponent<DependenciesAnalyzeManager.State> {
  private final Project myProject;
  private ContentManager myContentManager;

  public static class State {
    public boolean forwardDirection = true;
    public boolean includeTests = false;
  }

  private State myState = new State();

  public DependenciesAnalyzeManager(final Project project) {
    myProject = project;
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> {
      ToolWindowManager manager = ToolWindowManager.getInstance(myProject);
      ToolWindow toolWindow = manager.registerToolWindow(ToolWindowId.MODULES_DEPENDENCIES, true, ToolWindowAnchor.RIGHT, project);
      myContentManager = toolWindow.getContentManager();
      toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowModuleDependencies);
      new ContentManagerWatcher(toolWindow, myContentManager);
    });
  }

  public static DependenciesAnalyzeManager getInstance(Project project) {
    return ServiceManager.getService(project, DependenciesAnalyzeManager.class);
  }

  public void addContent(Content content) {
    myContentManager.addContent(content);
    myContentManager.setSelectedContent(content);
    ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.MODULES_DEPENDENCIES).activate(null);
  }

  public void closeContent(Content content) {
    myContentManager.removeContent(content, true);
  }

  @Override
  @NotNull
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    myState = state != null ? state : new State();
  }
}