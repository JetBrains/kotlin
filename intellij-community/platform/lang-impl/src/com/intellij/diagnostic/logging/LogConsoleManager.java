/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.diagnostic.logging;

import com.intellij.execution.configurations.AdditionalTabComponentManager;
import com.intellij.execution.configurations.RunConfigurationBase;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

public interface LogConsoleManager extends AdditionalTabComponentManager {
  void addLogConsole(@NotNull String name, @NotNull String path, @NotNull Charset charset, long skippedContent, @NotNull RunConfigurationBase runConfiguration);

  void removeLogConsole(@NotNull String pathOrId);
}
