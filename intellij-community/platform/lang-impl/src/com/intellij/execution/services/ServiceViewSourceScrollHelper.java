// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.services;

import com.intellij.execution.ExecutionBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.AutoScrollFromSourceHandler;
import com.intellij.ui.AutoScrollToSourceHandler;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

final class ServiceViewSourceScrollHelper {
  private static final String AUTO_SCROLL_TO_SOURCE_PROPERTY = "service.view.auto.scroll.to.source";
  private static final String AUTO_SCROLL_FROM_SOURCE_PROPERTY = "service.view.auto.scroll.from.source";

  @NotNull
  static AutoScrollToSourceHandler createAutoScrollToSourceHandler(@NotNull Project project) {
    return new ServiceViewAutoScrollToSourceHandler(project);
  }

  static void installAutoScrollSupport(@NotNull Project project, @NotNull ToolWindowEx toolWindow,
                                                            @NotNull AutoScrollToSourceHandler toSourceHandler) {
    ServiceViewAutoScrollFromSourceHandler fromSourceHandler = new ServiceViewAutoScrollFromSourceHandler(project, toolWindow);
    fromSourceHandler.install();
    DefaultActionGroup additionalGearActions = new DefaultActionGroup(toSourceHandler.createToggleAction(),
                                                                      fromSourceHandler.createToggleAction(),
                                                                      Separator.getInstance());
    List<AnAction> additionalProviderActions = ServiceViewActionProvider.getInstance().getAdditionalGearActions();
    for (AnAction action : additionalProviderActions) {
      additionalGearActions.add(action);
    }
    toolWindow.setAdditionalGearActions(additionalGearActions);
    toolWindow.setTitleActions(new ScrollFromEditorAction(fromSourceHandler));
  }

  private static boolean isAutoScrollFromSourceEnabled(@NotNull Project project) {
    return PropertiesComponent.getInstance(project).getBoolean(AUTO_SCROLL_FROM_SOURCE_PROPERTY, PlatformUtils.isDataGrip());
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
      PropertiesComponent.getInstance(myProject).setValue(AUTO_SCROLL_FROM_SOURCE_PROPERTY, enabled, PlatformUtils.isDataGrip());
    }

    @Override
    protected void selectElementFromEditor(@NotNull FileEditor editor) {
      select(editor);
    }

    private Promise<Void> select(@NotNull FileEditor editor) {
      VirtualFile virtualFile = FileEditorManagerEx.getInstanceEx(myProject).getFile(editor);
      if (virtualFile == null) {
        return Promises.rejectedPromise("Virtual file is null");
      }
      return ((ServiceViewManagerImpl)ServiceViewManager.getInstance(myProject)).select(virtualFile);
    }
  }

  private static class ScrollFromEditorAction extends DumbAwareAction {
    private final ServiceViewAutoScrollFromSourceHandler myScrollFromHandler;

    ScrollFromEditorAction(ServiceViewAutoScrollFromSourceHandler scrollFromHandler) {
      super(ExecutionBundle.message("service.view.scroll.from.editor.action.name"),
            ExecutionBundle.message("service.view.scroll.from.editor.action.description"),
            AllIcons.General.Locate);
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
      select(Arrays.asList(editors).iterator());
    }

    private void select(Iterator<FileEditor> editors) {
      if (!editors.hasNext()) return;

      FileEditor editor = editors.next();
      myScrollFromHandler.select(editor).onError(r -> select(editors));
    }
  }
}
