// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.handlers;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * This class customizes 'run anything' command execution settings depending on input command
 */
public abstract class RunAnythingCommandHandler {
  public static final ExtensionPointName<RunAnythingCommandHandler> EP_NAME =
    ExtensionPointName.create("com.intellij.runAnything.commandHandler");

  public abstract boolean isMatched(@NotNull String commandLine);

  /**
   * See {@link KillableProcessHandler#shouldKillProcessSoftly()} for details.
   */
  public boolean shouldKillProcessSoftly() {
    return true;
  }

  /**
   * Creates console builder for matched command
   */
  public abstract TextConsoleBuilder getConsoleBuilder(@NotNull Project project);

  @Nullable
  public static RunAnythingCommandHandler getMatchedHandler(@NotNull String commandLine) {
    return Arrays.stream(EP_NAME.getExtensions()).filter(handler -> handler.isMatched(commandLine)).findFirst().orElse(null);
  }
}