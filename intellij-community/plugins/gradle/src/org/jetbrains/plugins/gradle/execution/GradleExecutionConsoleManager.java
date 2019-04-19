/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.execution;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask;
import com.intellij.openapi.externalSystem.service.execution.DefaultExternalSystemExecutionConsoleManager;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemExecuteTaskTask;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemResolveProjectTask;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.execution.filters.ReRunSyncFilter;
import org.jetbrains.plugins.gradle.execution.filters.ReRunTaskFilter;
import org.jetbrains.plugins.gradle.util.GradleConstants;

/**
 * @author Vladislav.Soroka
 */
public class GradleExecutionConsoleManager extends DefaultExternalSystemExecutionConsoleManager {

  @NotNull
  @Override
  public ProjectSystemId getExternalSystemId() {
    return GradleConstants.SYSTEM_ID;
  }

  @Nullable
  @Override
  public ExecutionConsole attachExecutionConsole(@NotNull Project project,
                                                 @NotNull ExternalSystemTask task,
                                                 @Nullable ExecutionEnvironment env,
                                                 @Nullable ProcessHandler processHandler) {
    ConsoleView executionConsole = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
    executionConsole.attachToProcess(processHandler);
    if (task instanceof ExternalSystemExecuteTaskTask) {
      executionConsole.addMessageFilter(new ReRunTaskFilter((ExternalSystemExecuteTaskTask)task, env));
    }
    else if (task instanceof ExternalSystemResolveProjectTask) {
      executionConsole.addMessageFilter(new ReRunSyncFilter((ExternalSystemResolveProjectTask)task, project));
    }
    return executionConsole;
  }

  @Override
  public boolean isApplicableFor(@NotNull ExternalSystemTask task) {
    return GradleConstants.SYSTEM_ID.equals(task.getId().getProjectSystemId());
  }
}
