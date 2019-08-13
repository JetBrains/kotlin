// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl.statistics;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class BaseTestRunConfiguration extends LocatableConfigurationBase {

  private static final AtomicInteger CREATED_INSTANCES = new AtomicInteger(0);

  protected BaseTestRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
    super(project, factory, nextName());
  }

  private static String nextName() {
    return "Test #" + CREATED_INSTANCES.incrementAndGet();
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
    return null;
  }
}
