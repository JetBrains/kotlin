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
package org.jetbrains.plugins.gradle.execution.build;

import com.intellij.build.BuildViewManager;
import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.task.TaskCallback;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.task.*;
import com.intellij.task.impl.JpsProjectTaskRunner;
import com.intellij.util.CommonProcessors;
import com.intellij.util.SmartList;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.containers.MultiMap;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.GradleBuildSrcProjectsResolver;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.service.settings.GradleSettingsService;
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration.PROGRESS_LISTENER_KEY;
import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.*;
import static com.intellij.openapi.util.text.StringUtil.*;
import static org.jetbrains.plugins.gradle.execution.GradleRunnerUtil.resolveProjectPath;

/**
 * TODO automatically create exploded-war task
 * task explodedWar(type: Copy) {
 * into "$buildDir/explodedWar"
 * with war
 * }
 *
 * @author Vladislav.Soroka
 */
public class GradleProjectTaskRunner extends ProjectTaskRunner {

  @Language("Groovy")
  private static final String FORCE_COMPILE_TASKS_INIT_SCRIPT_TEMPLATE = "projectsEvaluated { \n" +
                                                                         "  rootProject.findProject('%s')?.tasks?.withType(AbstractCompile) {  \n" +
                                                                         "    outputs.upToDateWhen { false } \n" +
                                                                         "  } \n" +
                                                                         "}\n";

  @Override
  public void run(@NotNull Project project,
                  @NotNull ProjectTaskContext context,
                  @Nullable ProjectTaskNotification callback,
                  @NotNull Collection<? extends ProjectTask> tasks) {
    MultiMap<String, String> buildTasksMap = MultiMap.createLinkedSet();
    MultiMap<String, String> cleanTasksMap = MultiMap.createLinkedSet();
    MultiMap<String, String> initScripts = MultiMap.createLinkedSet();

    Map<Class<? extends ProjectTask>, List<ProjectTask>> taskMap = JpsProjectTaskRunner.groupBy(tasks);

    List<Module> modulesToBuild = addModulesBuildTasks(taskMap.get(ModuleBuildTask.class), buildTasksMap, initScripts);
    List<Module> modulesOfResourcesToBuild = addModulesBuildTasks(taskMap.get(ModuleResourcesBuildTask.class), buildTasksMap, initScripts);
    // TODO there should be 'gradle' way to build files instead of related modules entirely
    List<Module> modulesOfFiles = addModulesBuildTasks(taskMap.get(ModuleFilesBuildTask.class), buildTasksMap, initScripts);
    addArtifactsBuildTasks(taskMap.get(ProjectModelBuildTask.class), cleanTasksMap, buildTasksMap);

    // TODO send a message if nothing to build
    Set<String> rootPaths = buildTasksMap.keySet();
    AtomicInteger successCounter = new AtomicInteger();
    AtomicInteger errorCounter = new AtomicInteger();

    TaskCallback taskCallback = callback == null ? null : new TaskCallback() {
      @Override
      public void onSuccess() {
        handle(true);
      }

      @Override
      public void onFailure() {
        handle(false);
      }

      private void handle(boolean success) {
        int successes = success ? successCounter.incrementAndGet() : successCounter.get();
        int errors = success ? errorCounter.get() : errorCounter.incrementAndGet();
        if (successes + errors == rootPaths.size()) {
          if (!project.isDisposed()) {
            // refresh on output roots is required in order for the order enumerator to see all roots via VFS
            final List<Module> affectedModules = ContainerUtil.concat(modulesToBuild, modulesOfResourcesToBuild, modulesOfFiles);
            // have to refresh in case of errors too, because run configuration may be set to ignore errors
            Collection<String> affectedRoots = ContainerUtil.newHashSet(
              CompilerPathsEx.getOutputPaths(affectedModules.toArray(Module.EMPTY_ARRAY)));
            if (!affectedRoots.isEmpty()) {
              CompilerUtil.refreshOutputRoots(affectedRoots);
            }
          }
          callback.finished(new ProjectTaskResult(false, errors, 0));
        }
      }
    };

    String gradleVmOptions = GradleSettings.getInstance(project).getGradleVmOptions();
    for (String rootProjectPath : rootPaths) {
      Collection<String> buildTasks = buildTasksMap.get(rootProjectPath);
      if (buildTasks.isEmpty()) continue;
      Collection<String> cleanTasks = cleanTasksMap.get(rootProjectPath);

      ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();

      File projectFile = new File(rootProjectPath);
      final String projectName;
      if (projectFile.isFile()) {
        projectName = projectFile.getParentFile().getName();
      }
      else {
        projectName = projectFile.getName();
      }
      String executionName = "Build " + projectName;
      settings.setExecutionName(executionName);
      settings.setExternalProjectPath(rootProjectPath);
      settings.setTaskNames(ContainerUtil.collect(ContainerUtil.concat(cleanTasks, buildTasks).iterator()));
      //settings.setScriptParameters(scriptParameters);
      settings.setVmOptions(gradleVmOptions);
      settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());

      UserDataHolderBase userData = new UserDataHolderBase();
      userData.putUserData(PROGRESS_LISTENER_KEY, BuildViewManager.class);

      Collection<String> scripts = initScripts.getModifiable(rootProjectPath);
      userData.putUserData(GradleTaskManager.INIT_SCRIPT_KEY, join(scripts, SystemProperties.getLineSeparator()));
      userData.putUserData(GradleTaskManager.INIT_SCRIPT_PREFIX_KEY, executionName);

      ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID, project, GradleConstants.SYSTEM_ID,
                                 taskCallback, ProgressExecutionMode.IN_BACKGROUND_ASYNC, false, userData);
    }
  }

  @Override
  public boolean canRun(@NotNull ProjectTask projectTask) {
    if (projectTask instanceof ModuleBuildTask) {
      Module module = ((ModuleBuildTask)projectTask).getModule();
      if (!GradleSettingsService.isDelegatedBuildEnabled(module)) return false;
      return isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module);
    }
    if (projectTask instanceof ProjectModelBuildTask) {
      ProjectModelBuildTask buildTask = (ProjectModelBuildTask)projectTask;
      if (buildTask.getBuildableElement() instanceof Artifact) {
        for (GradleBuildTasksProvider buildTasksProvider : GradleBuildTasksProvider.EP_NAME.getExtensions()) {
          if (buildTasksProvider.isApplicable(buildTask)) return true;
        }
      }
    }

    if (projectTask instanceof ExecuteRunConfigurationTask) {
      RunProfile runProfile = ((ExecuteRunConfigurationTask)projectTask).getRunProfile();
      if (runProfile instanceof ModuleBasedConfiguration) {
        RunConfigurationModule module = ((ModuleBasedConfiguration)runProfile).getConfigurationModule();
        if (!isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module.getModule()) ||
            !GradleSettingsService.isDelegatedBuildEnabled(module.getModule())) {
          return false;
        }
      }
      for (GradleExecutionEnvironmentProvider environmentProvider : GradleExecutionEnvironmentProvider.EP_NAME.getExtensions()) {
        if (environmentProvider.isApplicable(((ExecuteRunConfigurationTask)projectTask))) {
          return true;
        }
      }
    }
    return false;
  }


  @Override
  public ExecutionEnvironment createExecutionEnvironment(@NotNull Project project,
                                                         @NotNull ExecuteRunConfigurationTask task,
                                                         @Nullable Executor executor) {
    for (GradleExecutionEnvironmentProvider environmentProvider : GradleExecutionEnvironmentProvider.EP_NAME.getExtensions()) {
      if (environmentProvider.isApplicable(task)) {
        return environmentProvider.createExecutionEnvironment(project, task, executor);
      }
    }
    return null;
  }

  private static List<Module> addModulesBuildTasks(@Nullable Collection<? extends ProjectTask> projectTasks,
                                                   @NotNull MultiMap<String, String> buildTasksMap,
                                                   @NotNull MultiMap<String, String> initScripts) {
    if (ContainerUtil.isEmpty(projectTasks)) return Collections.emptyList();

    List<Module> affectedModules = new SmartList<>();
    Map<Module, String> rootPathsMap = FactoryMap.create(module -> notNullize(resolveProjectPath(module)));
    final CachedModuleDataFinder moduleDataFinder = new CachedModuleDataFinder();
    for (ProjectTask projectTask : projectTasks) {
      if (!(projectTask instanceof ModuleBuildTask)) continue;

      ModuleBuildTask moduleBuildTask = (ModuleBuildTask)projectTask;
      collectAffectedModules(affectedModules, moduleBuildTask);

      Module module = moduleBuildTask.getModule();
      final String rootProjectPath = rootPathsMap.get(module);
      if (isEmpty(rootProjectPath)) continue;

      final String projectId = getExternalProjectId(module);
      if (projectId == null) continue;
      final String externalProjectPath = getExternalProjectPath(module);
      if (externalProjectPath == null || endsWith(externalProjectPath, "buildSrc")) continue;

      final DataNode<? extends ModuleData> moduleDataNode = moduleDataFinder.findMainModuleData(module);
      if (moduleDataNode == null) continue;

      // all buildSrc runtime projects will be built by gradle implicitly
      if (Boolean.parseBoolean(moduleDataNode.getData().getProperty(GradleBuildSrcProjectsResolver.BUILD_SRC_MODULE_PROPERTY))) {
        continue;
      }

      String gradlePath = GradleProjectResolverUtil.getGradlePath(module);
      if (gradlePath == null) continue;
      String taskPathPrefix = endsWithChar(gradlePath, ':') ? gradlePath : (gradlePath + ':');

      List<String> gradleModuleTasks = ContainerUtil.mapNotNull(
        findAll(moduleDataNode, ProjectKeys.TASK), node ->
          node.getData().isInherited() ? null : trimStart(node.getData().getName(), taskPathPrefix));

      Collection<String> projectInitScripts = initScripts.getModifiable(rootProjectPath);
      Collection<String> buildRootTasks = buildTasksMap.getModifiable(rootProjectPath);
      final String moduleType = getExternalModuleType(module);

      if (!moduleBuildTask.isIncrementalBuild() && !(moduleBuildTask instanceof ModuleFilesBuildTask)) {
        projectInitScripts.add(String.format(FORCE_COMPILE_TASKS_INIT_SCRIPT_TEMPLATE, gradlePath));
      }
      String assembleTask = "assemble";
      boolean buildOnlyResources = projectTask instanceof ModuleResourcesBuildTask;
      String buildTaskPrefix = buildOnlyResources ? "process" : "";
      String buildTaskSuffix = buildOnlyResources ? "resources" : "classes";
      if (GradleConstants.GRADLE_SOURCE_SET_MODULE_TYPE_KEY.equals(moduleType)) {
        String sourceSetName = GradleProjectResolverUtil.getSourceSetName(module);
        String gradleTask = getTaskName(buildTaskPrefix, buildTaskSuffix, sourceSetName);
        if (!addIfContains(taskPathPrefix, gradleTask, gradleModuleTasks, buildRootTasks) &&
            ("main".equals(sourceSetName) || "test".equals(sourceSetName))) {
          buildRootTasks.add(taskPathPrefix + assembleTask);
        }
      }
      else {
        String gradleTask = getTaskName(buildTaskPrefix, buildTaskSuffix, null);
        if (addIfContains(taskPathPrefix, gradleTask, gradleModuleTasks, buildRootTasks)) {
          String gradleTestTask = getTaskName(buildTaskPrefix, buildTaskSuffix, "test");
          addIfContains(taskPathPrefix, gradleTestTask, gradleModuleTasks, buildRootTasks);
        }
        else if (gradleModuleTasks.contains(assembleTask)) {
          buildRootTasks.add(taskPathPrefix + assembleTask);
        }
      }
    }
    return affectedModules;
  }

  private static void collectAffectedModules(@NotNull List<Module> affectedModules, @NotNull ModuleBuildTask moduleBuildTask) {
    Module module = moduleBuildTask.getModule();
    if (moduleBuildTask.isIncludeDependentModules()) {
      OrderEnumerator enumerator = ModuleRootManager.getInstance(module).orderEntries().recursively();
      if (!moduleBuildTask.isIncludeRuntimeDependencies()) {
        enumerator = enumerator.compileOnly();
      }
      enumerator.forEachModule(new CommonProcessors.CollectProcessor<>(affectedModules));
    }
    else {
      affectedModules.add(module);
    }
  }

  @NotNull
  private static String getTaskName(@NotNull String taskPrefix, @NotNull String taskSuffix, @Nullable String sourceSetName) {
    return isEmpty(sourceSetName) || "main".equals(sourceSetName) ?
           taskPrefix + (taskPrefix.isEmpty() ? taskSuffix : capitalize(taskSuffix)) :
           taskPrefix + (taskPrefix.isEmpty() ? sourceSetName : capitalize(sourceSetName)) + capitalize(taskSuffix);
  }

  private static boolean addIfContains(@NotNull String taskPathPrefix,
                                       @NotNull String gradleTask,
                                       @NotNull List<String> moduleTasks,
                                       @NotNull Collection<String> buildRootTasks) {
    if (moduleTasks.contains(gradleTask)) {
      buildRootTasks.add(taskPathPrefix + gradleTask);
      return true;
    }
    return false;
  }

  private static void addArtifactsBuildTasks(@Nullable Collection<? extends ProjectTask> tasks,
                                             @NotNull MultiMap<String, String> cleanTasksMap,
                                             @NotNull MultiMap<String, String> buildTasksMap) {
    if (ContainerUtil.isEmpty(tasks)) return;

    for (ProjectTask projectTask : tasks) {
      if (!(projectTask instanceof ProjectModelBuildTask)) continue;

      ProjectModelBuildTask projectModelBuildTask = (ProjectModelBuildTask)projectTask;
      for (GradleBuildTasksProvider buildTasksProvider : GradleBuildTasksProvider.EP_NAME.getExtensions()) {
        if (buildTasksProvider.isApplicable(projectModelBuildTask)) {
          buildTasksProvider.addBuildTasks(
            projectModelBuildTask,
              task -> cleanTasksMap.putValue(task.getLinkedExternalProjectPath(), task.getName()),
              task -> buildTasksMap.putValue(task.getLinkedExternalProjectPath(), task.getName())
            );
          }
        }
      }
  }
}
