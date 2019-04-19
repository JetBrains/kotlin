// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.hierarchy;

import com.intellij.icons.AllIcons;
import com.intellij.ide.impl.ContentManagerWatcher;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

@State(name = "HierarchyBrowserManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class HierarchyBrowserManager implements PersistentStateComponent<HierarchyBrowserManager.State> {
  public static class State {
    public boolean IS_AUTOSCROLL_TO_SOURCE;
    public boolean SORT_ALPHABETICALLY;
    public boolean HIDE_CLASSES_WHERE_METHOD_NOT_IMPLEMENTED;
    public String SCOPE;
    public String EXPORT_FILE_PATH;
  }

  private State myState = new State();
  private final ContentManager myContentManager;

  public HierarchyBrowserManager(final Project project) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = toolWindowManager.registerToolWindow(ToolWindowId.HIERARCHY, true, ToolWindowAnchor.RIGHT, project, true);

    myContentManager = toolWindow.getContentManager();
    toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowHierarchy);
    new ContentManagerWatcher(toolWindow, myContentManager);
  }

  public final ContentManager getContentManager() {
    return myContentManager;
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull final State state) {
    myState = state;
  }

  public static HierarchyBrowserManager getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, HierarchyBrowserManager.class);
  }

  public static State getSettings(@NotNull Project project) {
    State state = getInstance(project).getState();
    return state != null ? state : new State();
  }
}