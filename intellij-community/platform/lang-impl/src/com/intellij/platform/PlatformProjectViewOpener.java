// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.platform;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.extensions.ExtensionNotApplicableException;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class PlatformProjectViewOpener implements DirectoryProjectConfigurator {
  PlatformProjectViewOpener() {
    if (PlatformUtils.isPyCharmEducational() || PlatformUtils.isDataGrip()) {
      throw ExtensionNotApplicableException.INSTANCE;
    }
  }

  @Override
  public void configureProject(@NotNull Project project,
                               @NotNull VirtualFile baseDir,
                               @NotNull Ref<Module> moduleRef,
                               boolean isProjectCreatedWithWizard) {
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
    if (toolWindow == null) {
      MyListener listener = new MyListener(project);
      Disposer.register(project, listener);
      project.getMessageBus().connect(listener).subscribe(ToolWindowManagerListener.TOPIC, listener);
    }
    else {
      StartupManager.getInstance(project).runAfterOpened(() -> {
        activateProjectToolWindow(project, toolWindow);
      });
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

  private static final class MyListener implements ToolWindowManagerListener, Disposable {
    private final Project myProject;

    MyListener(@NotNull Project project) {
      myProject = project;
    }

    @Override
    public void toolWindowsRegistered(@NotNull List<String> id) {
      if (id.contains(ToolWindowId.PROJECT_VIEW)) {
        Disposer.dispose(this);
        activateProjectToolWindow(myProject, ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.PROJECT_VIEW));
      }
    }

    @Override
    public void dispose() {
    }
  }
}
