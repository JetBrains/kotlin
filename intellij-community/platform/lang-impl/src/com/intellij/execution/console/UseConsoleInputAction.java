// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class UseConsoleInputAction extends ToggleAction implements DumbAware {
  private final String processInputStateKey;
  private boolean useProcessStdIn;

  UseConsoleInputAction(@NotNull String processInputStateKey) {
    super("Use Console Input", null, AllIcons.Debugger.Console);

    this.processInputStateKey = processInputStateKey;
    useProcessStdIn = PropertiesComponent.getInstance().getBoolean(processInputStateKey);
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent event) {
    return !useProcessStdIn;
  }

  @Override
  public void setSelected(@NotNull AnActionEvent event, boolean state) {
    useProcessStdIn = !state;

    LanguageConsoleView consoleView = (LanguageConsoleView)event.getData(LangDataKeys.CONSOLE_VIEW);
    assert consoleView != null;
    DaemonCodeAnalyzer daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(consoleView.getProject());
    PsiFile file = consoleView.getFile();
    daemonCodeAnalyzer.setHighlightingEnabled(file, state);
    daemonCodeAnalyzer.restart(file);
    PropertiesComponent.getInstance().setValue(processInputStateKey, useProcessStdIn);

    List<AnAction> actions = ActionUtil.getActions(consoleView.getConsoleEditor().getComponent());
    ConsoleExecuteAction action = ContainerUtil.findInstance(actions, ConsoleExecuteAction.class);
    action.myExecuteActionHandler.myUseProcessStdIn = !state;
  }
}