// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListenerAdapter;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.impl.ToolWindowHeadlessManagerImpl;
import com.intellij.util.Consumer;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * We want to hide an external system tool window when last external project is unlinked from the current ide project
 * and show it when the first external project is linked to the ide project.
 * <p/>
 * This class encapsulates that functionality.
 */
public final class ExternalToolWindowManager {
  @SuppressWarnings("unchecked")
  public static void handle(@NotNull Project project) {
    for (final ExternalSystemManager<?, ?, ?, ?, ?> manager : ExternalSystemApiUtil.getAllManagers()) {
      final AbstractExternalSystemSettings settings = manager.getSettingsProvider().fun(project);
      settings.subscribe(new ExternalSystemSettingsListenerAdapter() {
        @Override
        public void onProjectsLinked(@NotNull Collection linked) {

          Consumer<ToolWindow> activate = (toolWindow) -> toolWindow.setAvailable(true, () -> {
            boolean shouldShow = settings.getLinkedProjectsSettings().size() == 1;
            if (shouldShow) {
              toolWindow.show(null);
            }
          });

          final ToolWindow toolWindow = getToolWindow(project, manager.getSystemId());
          if (toolWindow != null) {
            activate.consume(toolWindow);
          }
          else {
            StartupManager.getInstance(project).runAfterOpened(() -> {
              ApplicationManager.getApplication().invokeLater(() -> {
                ExternalSystemUtil.ensureToolWindowInitialized(project, manager.getSystemId());
                ToolWindowManager.getInstance(project).invokeLater(() -> {
                  if (project.isDisposed()) return;
                  ToolWindow toolWindow1 = getToolWindow(project, manager.getSystemId());
                  if (toolWindow1 != null) {
                    activate.consume(toolWindow1);
                  }
                });
              }, ModalityState.NON_MODAL, project.getDisposed());
            });
          }
        }

        @Override
        public void onProjectsUnlinked(@NotNull Set linkedProjectPaths) {
          if (!settings.getLinkedProjectsSettings().isEmpty()) {
            return;
          }
          final ToolWindow toolWindow = getToolWindow(project, manager.getSystemId());
          if (toolWindow != null) {
            UIUtil.invokeLaterIfNeeded(() -> toolWindow.setAvailable(false));
          }
        }
      });
    }
  }

  @Nullable
  public static ToolWindow getToolWindow(@NotNull Project project, @NotNull ProjectSystemId externalSystemId) {
    ToolWindow result = ToolWindowManager.getInstance(project).getToolWindow(externalSystemId.getReadableName());
    if (result == null && ApplicationManager.getApplication().isUnitTestMode()) {
      result = new ToolWindowHeadlessManagerImpl.MockToolWindow(project);
    }
    return result;
  }
}
