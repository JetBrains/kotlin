// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.execution;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RunAnythingRunProfile implements RunProfile {
  @NotNull private final String myOriginalCommand;
  @NotNull private final GeneralCommandLine myCommandLine;

  public RunAnythingRunProfile(@NotNull GeneralCommandLine commandLine,
                               @NotNull String originalCommand) {
    myCommandLine = commandLine;
    myOriginalCommand = originalCommand;
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
    return new RunAnythingRunProfileState(environment, myOriginalCommand);
  }

  @NotNull
  @Override
  public String getName() {
    return myOriginalCommand;
  }

  @NotNull
  public String getOriginalCommand() {
    return myOriginalCommand;
  }

  @NotNull
  public GeneralCommandLine getCommandLine() {
    return myCommandLine;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return AllIcons.Actions.Run_anything;
  }
}