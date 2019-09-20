/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.execution.actions;

import com.intellij.execution.KillableProcess;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Sergey Simonchik
 */
public class StopProcessAction extends DumbAwareAction implements AnAction.TransparentUpdate {
  private ProcessHandler myProcessHandler;

  public StopProcessAction(@NotNull String text, @Nullable String description, @Nullable ProcessHandler processHandler) {
    super(text, description, AllIcons.Actions.Suspend);
    myProcessHandler = processHandler;
  }

  public void setProcessHandler(@Nullable ProcessHandler processHandler) {
    myProcessHandler = processHandler;
  }

  @Override
  public void update(@NotNull final AnActionEvent e) {
    update(e.getPresentation(), getTemplatePresentation(), myProcessHandler);
  }

  public static void update(@NotNull Presentation presentation,
                            @NotNull Presentation templatePresentation,
                            @Nullable ProcessHandler processHandler) {
    boolean enable = false;
    Icon icon = templatePresentation.getIcon();
    String description = templatePresentation.getDescription();
    if (processHandler != null && !processHandler.isProcessTerminated()) {
      enable = true;
      if (processHandler.isProcessTerminating() && processHandler instanceof KillableProcess) {
        KillableProcess killableProcess = (KillableProcess) processHandler;
        if (killableProcess.canKillProcess()) {
          // 'force quite' action presentation
          icon = AllIcons.Debugger.KillProcess;
          description = "Kill process";
        }
      }
    }
    presentation.setEnabled(enable);
    presentation.setIcon(icon);
    presentation.setDescription(description);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    stopProcess(myProcessHandler);
  }

  public static void stopProcess(@Nullable ProcessHandler processHandler) {
    ExecutionManagerImpl.stopProcess(processHandler);
  }
}
