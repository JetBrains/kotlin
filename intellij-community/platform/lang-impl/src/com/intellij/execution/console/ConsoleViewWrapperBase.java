// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.ExecutionConsoleEx;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ConsoleViewWrapperBase implements ConsoleView, ExecutionConsoleEx {
  @NotNull
  private final ConsoleView myDelegate;

  public ConsoleViewWrapperBase(@NotNull ConsoleView delegate) {
    myDelegate = delegate;
  }

  @NotNull
  public ConsoleView getDelegate() {
    return myDelegate;
  }

  @Override
  public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType) {
    myDelegate.print(text, contentType);
  }

  @Override
  public void clear() {
    myDelegate.clear();
  }

  @Override
  public void scrollTo(int offset) {
    myDelegate.scrollTo(offset);
  }

  @Override
  public void attachToProcess(ProcessHandler processHandler) {
    myDelegate.attachToProcess(processHandler);
  }

  @Override
  public void setOutputPaused(boolean value) {
    myDelegate.setOutputPaused(value);
  }

  @Override
  public boolean isOutputPaused() {
    return myDelegate.isOutputPaused();
  }

  @Override
  public boolean hasDeferredOutput() {
    return myDelegate.hasDeferredOutput();
  }

  @Override
  public void performWhenNoDeferredOutput(@NotNull Runnable runnable) {
    myDelegate.performWhenNoDeferredOutput(runnable);
  }

  @Override
  public void setHelpId(@NotNull String helpId) {
    myDelegate.setHelpId(helpId);
  }

  @Override
  public void addMessageFilter(@NotNull Filter filter) {
    myDelegate.addMessageFilter(filter);
  }

  @Override
  public void printHyperlink(@NotNull String hyperlinkText, @Nullable HyperlinkInfo info) {
    myDelegate.printHyperlink(hyperlinkText, info);
  }

  @Override
  public int getContentSize() {
    return myDelegate.getContentSize();
  }

  @Override
  public boolean canPause() {
    return myDelegate.canPause();
  }

  @NotNull
  @Override
  public AnAction[] createConsoleActions() {
    return myDelegate.createConsoleActions();
  }

  @Override
  public void allowHeavyFilters() {
    myDelegate.allowHeavyFilters();
  }

  @Override
  public void buildUi(RunnerLayoutUi layoutUi) {
    if (myDelegate instanceof ExecutionConsoleEx) {
      ((ExecutionConsoleEx)myDelegate).buildUi(layoutUi);
    }
    else {
      RunContentBuilder.buildConsoleUiDefault(layoutUi, this);
    }
  }

  @Nullable
  @Override
  public String getExecutionConsoleId() {
    return myDelegate instanceof ExecutionConsoleEx
           ? ((ExecutionConsoleEx)myDelegate).getExecutionConsoleId()
           : null;
  }

  @Override
  public JComponent getComponent() {
    return myDelegate.getComponent();
  }

  @Override
  public JComponent getPreferredFocusableComponent() {
    return myDelegate.getPreferredFocusableComponent();
  }

  @Override
  public void dispose() {
    Disposer.dispose(myDelegate);
  }
}
