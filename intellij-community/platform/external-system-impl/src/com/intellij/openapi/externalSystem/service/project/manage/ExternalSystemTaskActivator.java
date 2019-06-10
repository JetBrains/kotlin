// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl.ExternalProjectsStateProvider;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.task.TaskCallback;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.task.ModuleBuildTask;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.ProjectTaskManager;
import com.intellij.task.ProjectTaskResult;
import com.intellij.task.impl.ProjectTaskManagerImpl;
import com.intellij.task.impl.ProjectTaskManagerListener;
import com.intellij.task.impl.ProjectTaskScope;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Vladislav.Soroka
 */
public class ExternalSystemTaskActivator {
  private static final Logger LOG = Logger.getInstance(ExternalSystemTaskActivator.class);

  public static final String RUN_CONFIGURATION_TASK_PREFIX = "run: ";
  @NotNull private final Project myProject;
  private final List<Listener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  public ExternalSystemTaskActivator(@NotNull Project project) {
    myProject = project;
  }

  @NotNull
  public static String getRunConfigurationActivationTaskName(@NotNull RunnerAndConfigurationSettings settings) {
    return RUN_CONFIGURATION_TASK_PREFIX + settings.getName();
  }

  public void init() {
    ProjectTaskManagerImpl projectTaskManager = (ProjectTaskManagerImpl)ProjectTaskManager.getInstance(myProject);
    projectTaskManager.addListener(new ProjectTaskManagerListener() {
      @Override
      public void beforeRun(@NotNull ProjectTaskContext context) throws ExecutionException {
        if (!doExecuteBuildPhaseTriggers(true, context)) {
          throw new ExecutionException("Before build triggering task failed");
        }
      }

      @Override
      public void afterRun(@NotNull ProjectTaskContext context, @NotNull ProjectTaskResult result) throws ExecutionException {
        if (!doExecuteBuildPhaseTriggers(false, context)) {
          throw new ExecutionException("After build triggering task failed");
        }
      }
    });

    fireTasksChanged();
  }

  public String getDescription(ProjectSystemId systemId, String projectPath, String taskName) {
    List<String> result = new ArrayList<>();
    final ExternalProjectsStateProvider stateProvider =
      ExternalProjectsManagerImpl.getInstance(myProject).getStateProvider();
    final TaskActivationState taskActivationState = stateProvider.getTasksActivation(systemId, projectPath);
    if (taskActivationState == null) {
      return null;
    }

    for (Phase phase : Phase.values()) {
      if (taskActivationState.getTasks(phase).contains(taskName)) {
        result.add(phase.toString());
      }
    }
    return StringUtil.join(result, ", ");
  }

  private boolean doExecuteBuildPhaseTriggers(boolean myBefore, @NotNull ProjectTaskContext context) {
    ProjectTaskScope taskScope = context.getUserData(ProjectTaskScope.KEY);
    if (taskScope == null) {
      return true;
    }

    Set<String> modulesToBuild = new LinkedHashSet<>();
    Set<String> modulesToRebuild = new LinkedHashSet<>();
    for (ModuleBuildTask task : taskScope.getRequestedTasks(ModuleBuildTask.class)) {
      String projectPath = ExternalSystemApiUtil.getExternalProjectPath(task.getModule());
      if (projectPath == null) continue;
      if (task.isIncrementalBuild()) {
        modulesToBuild.add(projectPath);
      }
      else {
        modulesToRebuild.add(projectPath);
      }
    }

    boolean result = true;
    if (myBefore) {
      if (!modulesToBuild.isEmpty()) {
        result = runTasks(modulesToBuild, Phase.BEFORE_COMPILE);
      }
      if (result && !modulesToRebuild.isEmpty()) {
        result = runTasks(modulesToRebuild, Phase.BEFORE_COMPILE, Phase.BEFORE_REBUILD);
      }
    }
    else {
      if (!modulesToBuild.isEmpty()) {
        result = runTasks(modulesToBuild, Phase.AFTER_COMPILE);
      }
      if (result && !modulesToRebuild.isEmpty()) {
        result = runTasks(modulesToRebuild, Phase.AFTER_COMPILE, Phase.AFTER_REBUILD);
      }
    }
    return result;
  }

  public boolean runTasks(@NotNull String modulePath, @NotNull Phase... phases) {
    return runTasks(Collections.singleton(modulePath), phases);
  }

  public boolean runTasks(@NotNull Collection<String> modules, @NotNull Phase... phases) {
    final ExternalProjectsStateProvider stateProvider =
      ExternalProjectsManagerImpl.getInstance(myProject).getStateProvider();

    final Queue<Pair<ProjectSystemId, ExternalSystemTaskExecutionSettings>> tasksQueue =
      new LinkedList<>();

    Map<ProjectSystemId, Map<String, RunnerAndConfigurationSettings>> lazyConfigurationsMap =
      FactoryMap.create(key -> {
        final AbstractExternalSystemTaskConfigurationType configurationType =
          ExternalSystemUtil.findConfigurationType(key);
        if (configurationType == null) {
          return null;
        }
        return ContainerUtil.map2Map(RunManager.getInstance(myProject).getConfigurationSettingsList(configurationType),
                                     configurationSettings1 -> Pair.create(configurationSettings1.getName(), configurationSettings1));
      });

    for (final ExternalProjectsStateProvider.TasksActivation activation : stateProvider.getAllTasksActivation()) {
      final boolean hashPath = modules.contains(activation.projectPath);

      final Set<String> tasks = new LinkedHashSet<>();
      for (Phase phase : phases) {
        List<String> activationTasks = activation.state.getTasks(phase);
        if (hashPath || (phase.isSyncPhase() && !activationTasks.isEmpty() &&  isShareSameRootPath(modules, activation))) {
          ContainerUtil.addAll(tasks, activationTasks);
        }
      }

      if (tasks.isEmpty()) {
        continue;
      }

      for (Iterator<String> iterator = tasks.iterator(); iterator.hasNext(); ) {
        String task = iterator.next();
        if (task.length() > RUN_CONFIGURATION_TASK_PREFIX.length() && task.startsWith(RUN_CONFIGURATION_TASK_PREFIX)) {
          iterator.remove();
          final String configurationName = task.substring(RUN_CONFIGURATION_TASK_PREFIX.length());

          Map<String, RunnerAndConfigurationSettings> settings = lazyConfigurationsMap.get(activation.systemId);
          if (settings == null) {
            continue;
          }

          RunnerAndConfigurationSettings configurationSettings = settings.get(configurationName);
          if (configurationSettings == null) {
            continue;
          }

          final RunConfiguration runConfiguration = configurationSettings.getConfiguration();
          if (configurationName.equals(configurationSettings.getName()) && runConfiguration instanceof ExternalSystemRunConfiguration) {
            tasksQueue.add(Pair.create(activation.systemId, ((ExternalSystemRunConfiguration)runConfiguration).getSettings()));
          }
        }
      }

      if (tasks.isEmpty()) {
        continue;
      }

      if (ExternalProjectsManager.getInstance(myProject).isIgnored(activation.systemId, activation.projectPath)) {
        continue;
      }

      ExternalSystemTaskExecutionSettings executionSettings = new ExternalSystemTaskExecutionSettings();
      executionSettings.setExternalSystemIdString(activation.systemId.toString());
      executionSettings.setExternalProjectPath(activation.projectPath);
      executionSettings.getTaskNames().addAll(tasks);
      tasksQueue.add(Pair.create(activation.systemId, executionSettings));
    }

    return runTasksQueue(tasksQueue);
  }

  private boolean isShareSameRootPath(@NotNull Collection<String> modules,
                                      @NotNull ExternalProjectsStateProvider.TasksActivation activation) {
    final AbstractExternalSystemSettings systemSettings = ExternalSystemApiUtil.getSettings(myProject, activation.systemId);
    final String rootProjectPath = getRootProjectPath(systemSettings, activation.projectPath);
    final List<String> rootPath = ContainerUtil.mapNotNull(modules, path -> getRootProjectPath(systemSettings, path));
    return rootPath.contains(rootProjectPath);
  }

  @Nullable
  private static String getRootProjectPath(@NotNull AbstractExternalSystemSettings systemSettings, @NotNull String projectPath) {
    final ExternalProjectSettings projectSettings = systemSettings.getLinkedProjectSettings(projectPath);
    return projectSettings != null ? projectSettings.getExternalProjectPath() : null;
  }

  private boolean runTasksQueue(final Queue<Pair<ProjectSystemId, ExternalSystemTaskExecutionSettings>> tasksQueue) {
    final Pair<ProjectSystemId, ExternalSystemTaskExecutionSettings> pair = tasksQueue.poll();
    if (pair == null) {
      return true;
    }

    final ProjectSystemId systemId = pair.first;
    final ExternalSystemTaskExecutionSettings executionSettings = pair.getSecond();

    final Semaphore targetDone = new Semaphore();
    targetDone.down();
    final Ref<Boolean> result = new Ref<>(false);
    ExternalSystemUtil.runTask(executionSettings, DefaultRunExecutor.EXECUTOR_ID, myProject, systemId,
                               new TaskCallback() {
                                 @Override
                                 public void onSuccess() {
                                   result.set(runTasksQueue(tasksQueue));
                                   targetDone.up();
                                 }

                                 @Override
                                 public void onFailure() {
                                   targetDone.up();
                                 }
                               },
                               ProgressExecutionMode.IN_BACKGROUND_ASYNC, false);
    targetDone.waitFor();
    return result.get();
  }

  public void addListener(@NotNull Listener l) {
    myListeners.add(l);
  }

  public boolean isTaskOfPhase(@NotNull TaskData taskData, @NotNull Phase phase) {
    final ExternalProjectsStateProvider stateProvider = ExternalProjectsManagerImpl.getInstance(myProject).getStateProvider();
    final TaskActivationState taskActivationState =
      stateProvider.getTasksActivation(taskData.getOwner(), taskData.getLinkedExternalProjectPath());
    if (taskActivationState == null) {
      return false;
    }

    return taskActivationState.getTasks(phase).contains(taskData.getName());
  }

  public void addTasks(@NotNull Collection<? extends TaskData> tasks, @NotNull final Phase phase) {
    if (tasks.isEmpty()) {
      return;
    }
    addTasks(ContainerUtil.map(tasks,
                               data -> new TaskActivationEntry(data.getOwner(), phase, data.getLinkedExternalProjectPath(), data.getName())));
    fireTasksChanged();
  }

  public void addTasks(@NotNull Collection<? extends TaskActivationEntry> entries) {
    if (entries.isEmpty()) {
      return;
    }

    final ExternalProjectsStateProvider stateProvider = ExternalProjectsManagerImpl.getInstance(myProject).getStateProvider();
    for (TaskActivationEntry entry : entries) {
      final TaskActivationState taskActivationState = stateProvider.getTasksActivation(entry.systemId, entry.projectPath);
      taskActivationState.getTasks(entry.phase).add(entry.taskName);
    }

    fireTasksChanged();
  }

  public void removeTasks(@NotNull Collection<? extends TaskData> tasks, @NotNull final Phase phase) {
    if (tasks.isEmpty()) {
      return;
    }
    removeTasks(ContainerUtil.map(tasks, data -> new TaskActivationEntry(data.getOwner(), phase, data.getLinkedExternalProjectPath(), data.getName())));
  }

  public void removeTasks(@NotNull Collection<? extends TaskActivationEntry> entries) {
    if (entries.isEmpty()) {
      return;
    }
    final ExternalProjectsStateProvider stateProvider = ExternalProjectsManagerImpl.getInstance(myProject).getStateProvider();
    for (TaskActivationEntry activationEntry : entries) {
      final TaskActivationState taskActivationState =
        stateProvider.getTasksActivation(activationEntry.systemId, activationEntry.projectPath);
      taskActivationState.getTasks(activationEntry.phase).remove(activationEntry.taskName);
    }
    fireTasksChanged();
  }

  public void addTask(@NotNull TaskActivationEntry entry) {
    addTasks(Collections.singleton(entry));
  }

  public void removeTask(@NotNull TaskActivationEntry entry) {
    removeTasks(Collections.singleton(entry));
  }


  public void moveTasks(@NotNull Collection<? extends TaskActivationEntry> entries, int increment) {
    LOG.assertTrue(increment == -1 || increment == 1);

    final ExternalProjectsStateProvider stateProvider = ExternalProjectsManagerImpl.getInstance(myProject).getStateProvider();
    for (TaskActivationEntry activationEntry : entries) {
      final TaskActivationState taskActivationState =
        stateProvider.getTasksActivation(activationEntry.systemId, activationEntry.projectPath);
      final List<String> tasks = taskActivationState.getTasks(activationEntry.phase);
      final int i1 = tasks.indexOf(activationEntry.taskName);
      final int i2 = i1 + increment;
      if (i1 != -1 && tasks.size() > i2 && i2 >= 0) {
        Collections.swap(tasks, i1, i2);
      }
    }
  }

  public void moveProjects(@NotNull ProjectSystemId systemId,
                           @NotNull List<String> projectsPathsToMove,
                           @Nullable Collection<String> pathsGroup,
                           int increment) {
    LOG.assertTrue(increment == -1 || increment == 1);

    final ExternalProjectsStateProvider stateProvider = ExternalProjectsManagerImpl.getInstance(myProject).getStateProvider();
    final Map<String, TaskActivationState> activationMap = stateProvider.getProjectsTasksActivationMap(systemId);
    final List<String> currentPaths = new ArrayList<>(activationMap.keySet());
    if (pathsGroup != null) {
      currentPaths.retainAll(pathsGroup);
    }

    for (String path : projectsPathsToMove) {
      final int i1 = currentPaths.indexOf(path);
      final int i2 = i1 + increment;
      if (i1 != -1 && currentPaths.size() > i2 && i2 >= 0) {
        Collections.swap(currentPaths, i1, i2);
      }
    }

    Map<String, TaskActivationState> rearrangedMap = new LinkedHashMap<>();
    for (String path : currentPaths) {
      rearrangedMap.put(path, activationMap.get(path));
      activationMap.remove(path);
    }
    activationMap.putAll(rearrangedMap);
  }


  public void fireTasksChanged() {
    for (Listener each : myListeners) {
      each.tasksActivationChanged();
    }
  }

  public enum Phase {
    BEFORE_RUN("external.system.task.before.run"),
    BEFORE_SYNC("external.system.task.before.sync"),
    AFTER_SYNC("external.system.task.after.sync"),
    BEFORE_COMPILE("external.system.task.before.compile"),
    AFTER_COMPILE("external.system.task.after.compile"),
    BEFORE_REBUILD("external.system.task.before.rebuild"),
    AFTER_REBUILD("external.system.task.after.rebuild");

    public final String myMessageKey;

    Phase(String messageKey) {
      myMessageKey = messageKey;
    }

    public boolean isSyncPhase () {
      return this == BEFORE_SYNC || this == AFTER_SYNC;
    }

    @Override
    public String toString() {
      return ExternalSystemBundle.message(myMessageKey);
    }
  }

  public interface Listener {
    void tasksActivationChanged();
  }

  public static class TaskActivationEntry {
    @NotNull private final ProjectSystemId systemId;
    @NotNull private final Phase phase;
    @NotNull private final String projectPath;
    @NotNull private final String taskName;

    public TaskActivationEntry(@NotNull ProjectSystemId systemId,
                               @NotNull Phase phase, @NotNull String projectPath, @NotNull String taskName) {
      this.systemId = systemId;
      this.phase = phase;
      this.projectPath = projectPath;
      this.taskName = taskName;
    }

    @NotNull
    public ProjectSystemId getSystemId() {
      return systemId;
    }

    @NotNull
    public Phase getPhase() {
      return phase;
    }

    @NotNull
    public String getProjectPath() {
      return projectPath;
    }

    @NotNull
    public String getTaskName() {
      return taskName;
    }
  }
}
