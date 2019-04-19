// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * Class NewWatchAction
 * @author Jeka
 */
package com.intellij.execution.ui.layout.actions;

import com.intellij.execution.ui.layout.impl.RunnerContentUi;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RestoreLayoutAction extends DumbAwareAction {
  @Nullable
  public static RunnerContentUi getRunnerUi(@NotNull AnActionEvent e) {
    return e.getData(RunnerContentUi.KEY);
  }

  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    getRunnerUi(e).restoreLayout();
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    e.getPresentation().setEnabled(getRunnerUi(e) != null);
  }
}