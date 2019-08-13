// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.ProjectSdkSetupValidator;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;

/**
 * @author Danila Ponomarenko
 */
public final class SdkSetupNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> implements DumbAware {
  public static final Key<EditorNotificationPanel> KEY = Key.create("SdkSetupNotification");

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {
    for (ProjectSdkSetupValidator validator : ProjectSdkSetupValidator.PROJECT_SDK_SETUP_VALIDATOR_EP.getExtensionList()) {
      if (validator.isApplicableFor(project, file)) {
        final String errorMessage = validator.getErrorMessage(project, file);
        if (errorMessage != null) {
          return createPanel(errorMessage, () -> validator.doFix(project, file));
        }
        return null;
      }
    }

    return null;
  }

  @NotNull
  private static EditorNotificationPanel createPanel(@NotNull String message, @NotNull Runnable fix) {
    EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.setText(message);
    panel.createActionLabel(ProjectBundle.message("project.sdk.setup"), fix);
    return panel;
  }
}