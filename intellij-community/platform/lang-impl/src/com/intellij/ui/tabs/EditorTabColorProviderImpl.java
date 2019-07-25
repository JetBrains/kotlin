// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ui.tabs;

import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.FileColorManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author spleaner
 */
public class EditorTabColorProviderImpl implements EditorTabColorProvider, DumbAware {

  @Override
  @Nullable
  public Color getEditorTabColor(@NotNull Project project, @NotNull VirtualFile file) {
    FileColorManager colorManager = FileColorManager.getInstance(project);
    return colorManager.isEnabledForTabs() ? colorManager.getFileColor(file) : null;
  }

  @Nullable
  @Override
  public Color getProjectViewColor(@NotNull Project project, @NotNull VirtualFile file) {
    FileColorManager colorManager = FileColorManager.getInstance(project);
    return colorManager.isEnabledForProjectView() ? colorManager.getFileColor(file) : null;
  }
}
