// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * Class NewWatchAction
 * @author Jeka
 */
package com.intellij.execution.ui.layout.actions;

import com.intellij.execution.ui.layout.impl.RunnerContentUi;
import com.intellij.openapi.actionSystem.ActionPlaces;
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
    RunnerContentUi ui = getRunnerUi(e);
    if (ui != null) {
      ui.restoreLayout();
    }
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    RunnerContentUi runnerContentUi = getRunnerUi(e);
    boolean enabled = false;
    if (runnerContentUi != null) {
      enabled = true;
      if (ActionPlaces.DEBUGGER_TOOLBAR.equals(e.getPlace())) {
        // In this case the action has to available in ActionPlaces.RUNNER_LAYOUT_BUTTON_TOOLBAR only
        enabled = !runnerContentUi.isMinimizeActionEnabled();
      }
    }
    e.getPresentation().setEnabledAndVisible(enabled);
  }
}