/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.externalSystem.service.internal;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.execution.ExternalTaskPojo;
import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.ExternalSystemFacadeManager;
import com.intellij.openapi.externalSystem.service.RemoteExternalSystemFacade;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager;
import com.intellij.openapi.externalSystem.service.remote.ExternalSystemProgressNotificationManagerImpl;
import com.intellij.openapi.externalSystem.service.remote.RemoteExternalSystemTaskManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.keyFMap.KeyFMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Zhdanov
 */
public class ExternalSystemExecuteTaskTask extends AbstractExternalSystemTask {

  @NotNull private final List<String> myTasksToExecute;
  @Nullable private final String myVmOptions;
  @Nullable private String myArguments;
  @Nullable private final String myJvmAgentSetup;
  private final boolean myPassParentEnvs;
  private final Map<String, String> myEnv;

  public ExternalSystemExecuteTaskTask(@NotNull Project project,
                                       @NotNull ExternalSystemTaskExecutionSettings settings,
                                       @Nullable String jvmAgentSetup) throws IllegalArgumentException {
    super(settings.getExternalSystemId(), ExternalSystemTaskType.EXECUTE_TASK, project, settings.getExternalProjectPath());
    myTasksToExecute = ContainerUtilRt.newArrayList(settings.getTaskNames());
    myVmOptions = settings.getVmOptions();
    myArguments = settings.getScriptParameters();
    myPassParentEnvs = settings.isPassParentEnvs();
    myEnv = settings.getEnv();
    myJvmAgentSetup = jvmAgentSetup;
  }

  /**
   * @deprecated use {@link #ExternalSystemExecuteTaskTask(Project, ExternalSystemTaskExecutionSettings, String)}
   */
  @Deprecated
  public ExternalSystemExecuteTaskTask(@NotNull ProjectSystemId externalSystemId,
                                       @NotNull Project project,
                                       @NotNull List<? extends ExternalTaskPojo> tasksToExecute,
                                       @Nullable String vmOptions,
                                       @Nullable String arguments,
                                       @Nullable String jvmAgentSetup) throws IllegalArgumentException {
    super(externalSystemId, ExternalSystemTaskType.EXECUTE_TASK, project, getLinkedExternalProjectPath(tasksToExecute));
    myTasksToExecute = ContainerUtil.map(tasksToExecute, ExternalTaskPojo::getName);
    myVmOptions = vmOptions;
    myArguments = arguments;
    myJvmAgentSetup = jvmAgentSetup;
    myPassParentEnvs = true;
    myEnv = Collections.emptyMap();
  }

  @NotNull
  public List<String> getTasksToExecute() {
    return myTasksToExecute;
  }

  @Nullable
  public String getVmOptions() {
    return myVmOptions;
  }

  @Nullable
  public String getArguments() {
    return myArguments;
  }

  public void appendArguments(@NotNull String arguments) {
    myArguments = myArguments == null ? arguments : myArguments + ' ' + arguments;
  }

  @Deprecated
  @NotNull
  private static String getLinkedExternalProjectPath(@NotNull Collection<? extends ExternalTaskPojo> tasks) throws IllegalArgumentException {
    if (tasks.isEmpty()) {
      throw new IllegalArgumentException("Can't execute external tasks. Reason: given tasks list is empty");
    }
    String result = null;
    for (ExternalTaskPojo task : tasks) {
      String path = task.getLinkedExternalProjectPath();
      if (result == null) {
        result = path;
      }
      else if (!result.equals(path)) {
        throw new IllegalArgumentException(String.format(
          "Can't execute given external system tasks. Reason: expected that all of them belong to the same external project " +
          "but they are not (at least two different projects detected - '%s' and '%s'). Tasks: %s",
          result,
          task.getLinkedExternalProjectPath(),
          tasks
        ));
      }
    }
    assert result != null;
    return result;
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doExecute() throws Exception {
    ExternalSystemProgressNotificationManagerImpl progressNotificationManager =
      (ExternalSystemProgressNotificationManagerImpl)ServiceManager.getService(ExternalSystemProgressNotificationManager.class);
    ExternalSystemTaskId id = getId();
    String projectPath = getExternalProjectPath();

    ExternalSystemExecutionSettings settings;
    RemoteExternalSystemTaskManager taskManager;
    try {
      progressNotificationManager.onStart(id, projectPath);

      final ExternalSystemFacadeManager manager = ServiceManager.getService(ExternalSystemFacadeManager.class);
      settings = ExternalSystemApiUtil.getExecutionSettings(getIdeProject(),
                                                            projectPath,
                                                            getExternalSystemId());
      KeyFMap keyFMap = getUserMap();
      for (Key key : keyFMap.getKeys()) {
        settings.putUserData(key, keyFMap.get(key));
      }

      RemoteExternalSystemFacade facade = manager.getFacade(getIdeProject(), projectPath, getExternalSystemId());
      taskManager = facade.getTaskManager();
      final List<String> vmOptions = parseCmdParameters(myVmOptions);
      final List<String> arguments = parseCmdParameters(myArguments);
      settings
        .withVmOptions(vmOptions)
        .withArguments(arguments)
        .withEnvironmentVariables(myEnv)
        .passParentEnvs(myPassParentEnvs);
    }
    catch (Exception e) {
      progressNotificationManager.onFailure(id, e);
      progressNotificationManager.onEnd(id);
      throw e;
    }

    taskManager.executeTasks(id, myTasksToExecute, projectPath, settings, myJvmAgentSetup);
  }

  @Override
  protected boolean doCancel() throws Exception {
    final ExternalSystemFacadeManager manager = ServiceManager.getService(ExternalSystemFacadeManager.class);
    RemoteExternalSystemFacade facade = manager.getFacade(getIdeProject(), getExternalProjectPath(), getExternalSystemId());
    RemoteExternalSystemTaskManager taskManager = facade.getTaskManager();

    return taskManager.cancelTask(getId());
  }

  private static List<String> parseCmdParameters(@Nullable String cmdArgsLine) {
    return cmdArgsLine != null ? ParametersListUtil.parse(cmdArgsLine, false, true) : ContainerUtil.newArrayList();
  }
}
