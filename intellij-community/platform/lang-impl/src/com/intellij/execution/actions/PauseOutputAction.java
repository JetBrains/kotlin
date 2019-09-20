// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.actions;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PauseOutputAction extends ToggleAction implements DumbAware {
    public PauseOutputAction() {
      super(ExecutionBundle.message("run.configuration.pause.output.action.name"), null, AllIcons.Actions.Pause);
    }

    @Nullable
    private static ConsoleView getConsoleView(AnActionEvent event) {
      return event.getData(LangDataKeys.CONSOLE_VIEW);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent event) {
      ConsoleView consoleView = getConsoleView(event);
      return consoleView != null && consoleView.isOutputPaused();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent event, boolean flag) {
      ConsoleView consoleView = getConsoleView(event);
      if (consoleView != null) {
        consoleView.setOutputPaused(flag);
      }
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
      super.update(event);
      final Presentation presentation = event.getPresentation();
      ConsoleView consoleView = getConsoleView(event);
      if (consoleView == null || !consoleView.canPause()) {
        presentation.setEnabled(false);
      } else {
        RunContentDescriptor descriptor = StopAction.getRecentlyStartedContentDescriptor(event.getDataContext());
        ProcessHandler handler = descriptor != null ? descriptor.getProcessHandler() : null;
        if (handler != null && !handler.isProcessTerminated()) {
          presentation.setEnabled(true);
        } else if (consoleView.hasDeferredOutput()) {
          presentation.setEnabled(true);
        } else {
          presentation.setEnabled(false);
        }
      }

    }
  }