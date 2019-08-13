// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.runAnything.RunAnythingUtil;
import com.intellij.ide.actions.runAnything.handlers.RunAnythingCommandHandler;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class RunAnythingRunProfileState extends CommandLineState {
  public RunAnythingRunProfileState(@NotNull ExecutionEnvironment environment, @NotNull String originalCommand) {
    super(environment);

    RunAnythingCommandHandler handler = RunAnythingCommandHandler.getMatchedHandler(originalCommand);
    if (handler != null) {
      setConsoleBuilder(handler.getConsoleBuilder(environment.getProject()));
    }
  }

  @NotNull
  private RunAnythingRunProfile getRunProfile() {
    RunProfile runProfile = getEnvironment().getRunProfile();
    if (!(runProfile instanceof RunAnythingRunProfile)) {
      throw new IllegalStateException("Got " + runProfile + " instead of RunAnything profile");
    }
    return (RunAnythingRunProfile)runProfile;
  }

  @NotNull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    RunAnythingRunProfile runProfile = getRunProfile();
    GeneralCommandLine commandLine = runProfile.getCommandLine();
    String originalCommand = runProfile.getOriginalCommand();
    KillableColoredProcessHandler processHandler = new KillableColoredProcessHandler(commandLine) {
      @Override
      protected void notifyProcessTerminated(int exitCode) {
        print(IdeBundle.message("run.anything.console.process.finished", exitCode), ConsoleViewContentType.SYSTEM_OUTPUT);
        super.notifyProcessTerminated(exitCode);
      }

      @Override
      public final boolean shouldKillProcessSoftly() {
        RunAnythingCommandHandler handler = RunAnythingCommandHandler.getMatchedHandler(originalCommand);
        return handler != null ? handler.shouldKillProcessSoftly() : super.shouldKillProcessSoftly();
      }

      private void print(@NotNull String message, @NotNull ConsoleViewContentType consoleViewContentType) {
        ConsoleView console = getConsoleView();
        if (console != null) console.print(message, consoleViewContentType);
      }

      @Nullable
      private ConsoleView getConsoleView() {
        RunContentDescriptor contentDescriptor =
          ExecutionManager.getInstance(getEnvironment().getProject())
                          .getContentManager()
                          .findContentDescriptor(getEnvironment().getExecutor(), this);

        ConsoleView console = null;
        if (contentDescriptor != null && contentDescriptor.getExecutionConsole() instanceof ConsoleView) {
          console = (ConsoleView)contentDescriptor.getExecutionConsole();
        }
        return console;
      }
    };

    processHandler.addProcessListener(new ProcessAdapter() {
      boolean myIsFirstLineAdded;

      @Override
      public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        if (!myIsFirstLineAdded) {
          Objects.requireNonNull(RunAnythingUtil.getOrCreateWrappedCommands(getEnvironment().getProject()))
                 .add(Pair.create(StringUtil.trim(event.getText()), originalCommand));
          myIsFirstLineAdded = true;
        }
      }
    });
    processHandler.setHasPty(true);
    return processHandler;
  }
}
