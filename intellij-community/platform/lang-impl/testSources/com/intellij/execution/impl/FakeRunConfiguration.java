/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.execution.impl;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public class FakeRunConfiguration extends LocatableConfigurationBase {

  private static final AtomicInteger CREATED_INSTANCES = new AtomicInteger(0);

  private final boolean mySurviveSoftKill;

  protected FakeRunConfiguration(@NotNull Project project, boolean surviveSoftKill) {
    super(project, FakeConfigurationFactory.INSTANCE, nextName());
    mySurviveSoftKill = surviveSoftKill;
  }

  private static String nextName() {
    return "Fake #" + CREATED_INSTANCES.incrementAndGet();
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
    return new FakeRunProfileState();
  }

  public class FakeRunProfileState implements RunProfileState {
    @Nullable
    @Override
    public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) {
      return new DefaultExecutionResult(null, new FakeProcessHandler(mySurviveSoftKill));
    }
  }
}
