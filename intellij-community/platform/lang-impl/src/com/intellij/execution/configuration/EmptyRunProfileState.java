package com.intellij.execution.configuration;

import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ProgramRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class EmptyRunProfileState implements RunProfileState {
  public static final RunProfileState INSTANCE = new EmptyRunProfileState();

  private EmptyRunProfileState() {
  }

  @Nullable
  @Override
  public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) {
    return null;
  }
}