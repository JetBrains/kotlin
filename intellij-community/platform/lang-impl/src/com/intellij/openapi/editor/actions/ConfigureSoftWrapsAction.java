// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.editor.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;

import static com.intellij.ide.actions.ShowSettingsUtilImpl.showSettingsDialog;

public class ConfigureSoftWrapsAction extends DumbAwareAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    showSettingsDialog(e.getProject(), "preferences.editor", "soft wraps");
  }
}
