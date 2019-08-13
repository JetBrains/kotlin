// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Vladislav.Soroka
 */
public class BuildContentDescriptor extends RunContentDescriptor {
  private boolean activateToolWindowWhenFailed = true;

  public BuildContentDescriptor(@Nullable ExecutionConsole executionConsole,
                                @Nullable ProcessHandler processHandler,
                                @NotNull JComponent component,
                                String displayName) {
    super(executionConsole, processHandler, component, displayName);
  }

  public boolean isActivateToolWindowWhenFailed() {
    return activateToolWindowWhenFailed;
  }

  public void setActivateToolWindowWhenFailed(boolean activateToolWindowWhenFailed) {
    this.activateToolWindowWhenFailed = activateToolWindowWhenFailed;
  }
}
