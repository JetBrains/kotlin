/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.tools;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public class ToolBeforeRunTaskProvider extends AbstractToolBeforeRunTaskProvider<ToolBeforeRunTask> {
  static final Key<ToolBeforeRunTask> ID = Key.create("ToolBeforeRunTask");

  @Override
  public Key<ToolBeforeRunTask> getId() {
    return ID;
  }

  @Override
  public String getName() {
    return ToolsBundle.message("tools.before.run.provider.name");
  }

  @Override
  public ToolBeforeRunTask createTask(@NotNull RunConfiguration runConfiguration) {
    return new ToolBeforeRunTask();
  }

  @Override
  protected ToolsPanel createToolsPanel() {
    return new ToolsPanel();
  }
}
