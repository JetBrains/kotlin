// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.platform;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.extensions.ExtensionNotApplicableException;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;

public class PlatformProjectViewOpener implements DirectoryProjectConfigurator {
  public PlatformProjectViewOpener() {
    if (PlatformUtils.isPyCharmEducational() || PlatformUtils.isDataGrip()) {
      throw ExtensionNotApplicableException.INSTANCE;
    }
  }

  @Override
  public void configureProject(@NotNull final Project project,
                               @NotNull final VirtualFile baseDir,
                               @NotNull Ref<Module> moduleRef,
                               boolean newProject) {
    ToolWindowManagerEx manager = (ToolWindowManagerEx)ToolWindowManager.getInstance(project);
    ToolWindow toolWindow = manager.getToolWindow(ToolWindowId.PROJECT_VIEW);
    if (toolWindow == null) {
      project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, new MyListener(manager, project));
    }
    else {
      StartupManager.getInstance(project).runWhenProjectIsInitialized(
        (DumbAwareRunnable)() -> activateProjectToolWindow(project, toolWindow));
    }
  }

  private static void activateProjectToolWindow(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (project.isDisposed()) return;
      if (toolWindow.getType() != ToolWindowType.SLIDING) {
        toolWindow.activate(null);
      }
    }, ModalityState.NON_MODAL);
  }

  private static class MyListener implements ToolWindowManagerListener {
    private final ToolWindowManagerEx myManager;
    private final Project myProject;

    MyListener(ToolWindowManagerEx manager, Project project) {
      myManager = manager;
      myProject = project;
    }

    @Override
    public void toolWindowRegistered(@NotNull final String id) {
      if (id.equals(ToolWindowId.PROJECT_VIEW)) {
        myManager.removeToolWindowManagerListener(this);
        activateProjectToolWindow(myProject, myManager.getToolWindow(id));
      }
    }
  }
}
