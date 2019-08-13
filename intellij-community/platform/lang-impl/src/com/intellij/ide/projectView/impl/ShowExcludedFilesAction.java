// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.scopeView.ScopeViewPane;
import org.jetbrains.annotations.NotNull;

import static com.intellij.ide.IdeBundle.message;

final class ShowExcludedFilesAction extends ProjectViewToggleAction {
  static final ShowExcludedFilesAction INSTANCE = new ShowExcludedFilesAction();

  ShowExcludedFilesAction() {
    super(message("action.show.excluded.files"), message("action.show.hide.excluded.files"));
  }

  @Override
  boolean isSupported(@NotNull ProjectViewImpl view, @NotNull String id) {
    return id.equals(ProjectViewPane.ID) || id.equals(ScopeViewPane.ID);
  }

  @Override
  boolean isSelected(@NotNull ProjectViewImpl view, @NotNull String id) {
    return view.isShowExcludedFiles(id);
  }

  @Override
  void setSelected(@NotNull ProjectViewImpl view, @NotNull String id, boolean showExcludedFiles) {
    view.setShowExcludedFiles(id, showExcludedFiles, true);
  }
}
