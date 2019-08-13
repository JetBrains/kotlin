// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.AutoScrollFromSourceHandler;
import com.intellij.ui.AutoScrollToSourceHandler;
import org.jetbrains.annotations.NotNull;

class ServiceViewSourceScrollHelper {
  private static final String AUTO_SCROLL_TO_SOURCE_PROPERTY = "service.view.auto.scroll.to.source";
  private static final String AUTO_SCROLL_FROM_SOURCE_PROPERTY = "service.view.auto.scroll.from.source";

  static AutoScrollToSourceHandler installAutoScrollSupport(@NotNull Project project, @NotNull ToolWindowEx toolWindow) {
    AutoScrollToSourceHandler toSourceHandler = new ServiceViewAutoScrollToSourceHandler(project);
    ServiceViewAutoScrollFromSourceHandler fromSourceHandler = new ServiceViewAutoScrollFromSourceHandler(project, toolWindow);
    fromSourceHandler.install();
    toolWindow.setAdditionalGearActions(new DefaultActionGroup(toSourceHandler.createToggleAction(),
                                                               fromSourceHandler.createToggleAction()));
    toolWindow.setTitleActions(new ScrollFromEditorAction(fromSourceHandler));
    return toSourceHandler;
  }

  private static boolean isAutoScrollFromSourceEnabled(@NotNull Project project) {
    return PropertiesComponent.getInstance(project).getBoolean(AUTO_SCROLL_FROM_SOURCE_PROPERTY);
  }

  private static class ServiceViewAutoScrollToSourceHandler extends AutoScrollToSourceHandler {
    private final Project myProject;

    ServiceViewAutoScrollToSourceHandler(@NotNull Project project) {
      myProject = project;
    }

    @Override
    protected boolean isAutoScrollMode() {
      return PropertiesComponent.getInstance(myProject).getBoolean(AUTO_SCROLL_TO_SOURCE_PROPERTY);
    }

    @Override
    protected void setAutoScrollMode(boolean state) {
      PropertiesComponent.getInstance(myProject).setValue(AUTO_SCROLL_TO_SOURCE_PROPERTY, state);
    }
  }

  private static class ServiceViewAutoScrollFromSourceHandler extends AutoScrollFromSourceHandler {
    ServiceViewAutoScrollFromSourceHandler(@NotNull Project project, @NotNull ToolWindow toolWindow) {
      super(project, toolWindow.getComponent(), toolWindow.getContentManager());
    }

    @Override
    protected boolean isAutoScrollEnabled() {
      return isAutoScrollFromSourceEnabled(myProject);
    }

    @Override
    protected void setAutoScrollEnabled(boolean enabled) {
      PropertiesComponent.getInstance(myProject).setValue(AUTO_SCROLL_FROM_SOURCE_PROPERTY, enabled);
    }

    @Override
    protected void selectElementFromEditor(@NotNull FileEditor editor) {
      select(editor);
    }

    private boolean select(@NotNull FileEditor editor) {
      for (ServiceViewContributor extension : ServiceModel.getContributors()) {
        if (!(extension instanceof ServiceViewFileEditorContributor)) continue;
        if (selectContributorNode(editor, (ServiceViewFileEditorContributor)extension, extension.getClass())) return true;
      }
      return false;
    }

    private boolean selectContributorNode(@NotNull FileEditor editor, @NotNull ServiceViewFileEditorContributor extension,
                                          @NotNull Class<?> contributorClass) {
      Object service = extension.findService(myProject, editor);
      if (service == null) return false;
      if (service instanceof ServiceViewFileEditorContributor) {
        if (selectContributorNode(editor, (ServiceViewFileEditorContributor)service, contributorClass)) return true;
      }
      ServiceViewManager.getInstance(myProject).select(service, contributorClass, true, true);
      return true;
    }
  }

  private static class ScrollFromEditorAction extends DumbAwareAction {
    private final ServiceViewAutoScrollFromSourceHandler myScrollFromHandler;

    ScrollFromEditorAction(ServiceViewAutoScrollFromSourceHandler scrollFromHandler) {
      super("Scroll from Source", "Select node open in the active editor", AllIcons.General.Locate);
      myScrollFromHandler = scrollFromHandler;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      if (project == null) {
        e.getPresentation().setEnabledAndVisible(false);
        return;
      }
      e.getPresentation().setEnabledAndVisible(!isAutoScrollFromSourceEnabled(project));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      if (project == null) return;
      FileEditorManager manager = FileEditorManager.getInstance(project);
      FileEditor[] editors = manager.getSelectedEditors();
      if (editors.length == 0) return;
      for (FileEditor editor : editors) {
        if (myScrollFromHandler.select(editor)) return;
      }
    }
  }
}
