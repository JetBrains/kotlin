// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution;

import com.intellij.execution.filters.Filter;
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
    Filter[] filters = getCustomExecutionFilters(project, task, env);
    for (Filter filter : filters) {
      executionConsole.addMessageFilter(filter);
    }
    return executionConsole;
  }

  @Override
  public boolean isApplicableFor(@NotNull ExternalSystemTask task) {
    return GradleConstants.SYSTEM_ID.equals(task.getId().getProjectSystemId());
  }

  @Override
  public Filter[] getCustomExecutionFilters(@NotNull Project project,
                                            @NotNull ExternalSystemTask task,
                                            @Nullable ExecutionEnvironment env) {
    if (task instanceof ExternalSystemExecuteTaskTask) {
      return new Filter[]{new ReRunTaskFilter((ExternalSystemExecuteTaskTask)task, env)};
    }
    else if (task instanceof ExternalSystemResolveProjectTask) {
      return new Filter[]{new ReRunSyncFilter((ExternalSystemResolveProjectTask)task, project)};
    }
    return Filter.EMPTY_ARRAY;
  }
}
