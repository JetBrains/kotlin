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
package com.intellij.task.impl;

import com.intellij.compiler.impl.CompileContextImpl;
import com.intellij.compiler.impl.CompileDriver;
import com.intellij.compiler.impl.CompileScopeUtil;
import com.intellij.compiler.impl.CompositeScope;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectModelBuildableElement;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsWorkspaceSettings;
import com.intellij.task.*;
import com.intellij.ui.GuiUtils;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Vladislav.Soroka
 */
public class JpsProjectTaskRunner extends ProjectTaskRunner {
  private static final Logger LOG = Logger.getInstance(JpsProjectTaskRunner.class);
  public static final Key<Object> EXECUTION_SESSION_ID_KEY = ExecutionManagerImpl.EXECUTION_SESSION_ID_KEY;

  @Override
  public void run(@NotNull Project project,
                  @NotNull ProjectTaskContext context,
                  @Nullable ProjectTaskNotification callback,
                  @NotNull Collection<? extends ProjectTask> tasks) {
    MessageBusConnection connection = project.getMessageBus().connect(project);
    connection.subscribe(CompilerTopics.COMPILATION_STATUS, new CompilationStatusListener() {
      @Override
      public void fileGenerated(@NotNull String outputRoot, @NotNull String relativePath) {
        context.fileGenerated(outputRoot, relativePath);
      }
    });
    CompileStatusNotification compileNotification = (aborted, errors, warnings, compileContext) -> {
      context.putUserData(CompileContextImpl.CONTEXT_KEY, compileContext);
      if (callback != null) {
        callback.finished(new ProjectTaskResult(aborted, errors, warnings));
      }
      connection.disconnect();
    };

    Map<Class<? extends ProjectTask>, List<ProjectTask>> taskMap = groupBy(tasks);
    GuiUtils.invokeLaterIfNeeded(() -> {
      runModulesResourcesBuildTasks(project, context, compileNotification, taskMap);
      runModulesBuildTasks(project, context, compileNotification, taskMap);
      runFilesBuildTasks(project, compileNotification, taskMap);
      runEmptyBuildTask(project, context, compileNotification, taskMap);
      runArtifactsBuildTasks(project, context, compileNotification, taskMap);
    }, ModalityState.defaultModalityState(), project.getDisposed());
  }

  @Override
  public boolean canRun(@NotNull ProjectTask projectTask) {
    return projectTask instanceof ModuleBuildTask || projectTask instanceof EmptyCompileScopeBuildTask || 
           (projectTask instanceof ProjectModelBuildTask && ((ProjectModelBuildTask)projectTask).getBuildableElement() instanceof Artifact);
  }

  public static Map<Class<? extends ProjectTask>, List<ProjectTask>> groupBy(@NotNull Collection<? extends ProjectTask> tasks) {
    return tasks.stream().collect(Collectors.groupingBy(o -> {
      if (o instanceof ModuleFilesBuildTask) return ModuleFilesBuildTask.class;
      if (o instanceof ModuleResourcesBuildTask) return ModuleResourcesBuildTask.class;
      if (o instanceof ModuleBuildTask) return ModuleBuildTask.class;
      if (o instanceof ProjectModelBuildTask) return ProjectModelBuildTask.class;
      if (o instanceof EmptyCompileScopeBuildTask) return EmptyCompileScopeBuildTask.class;
      return o.getClass();
    }));
  }

  private static void runModulesBuildTasks(@NotNull Project project,
                                           @NotNull ProjectTaskContext context,
                                           @NotNull CompileStatusNotification compileNotification,
                                           @NotNull Map<Class<? extends ProjectTask>, List<ProjectTask>> tasksMap) {
    Collection<? extends ProjectTask> buildTasks = tasksMap.get(ModuleBuildTask.class);
    if (ContainerUtil.isEmpty(buildTasks)) return;

    ModulesBuildSettings modulesBuildSettings = assembleModulesBuildSettings(buildTasks);
    CompilerManager compilerManager = CompilerManager.getInstance(project);
    
    if (modulesBuildSettings.isRebuild()){
      compilerManager.rebuild(compileNotification);
    }
    else {
      CompileScope scope = createScope(
        compilerManager, context, modulesBuildSettings.modules, modulesBuildSettings.includeDependentModules, modulesBuildSettings.includeRuntimeDependencies
      );
      if (modulesBuildSettings.isIncrementalBuild) {
        compilerManager.make(scope, compileNotification);
      }
      else {
        compilerManager.compile(scope, compileNotification);
      }
    }
  }
  private static void runEmptyBuildTask(@NotNull Project project,
                                           @NotNull ProjectTaskContext context,
                                           @NotNull CompileStatusNotification compileNotification,
                                           @NotNull Map<Class<? extends ProjectTask>, List<ProjectTask>> tasksMap) {
    Collection<? extends ProjectTask> buildTasks = tasksMap.get(EmptyCompileScopeBuildTask.class);
    if (ContainerUtil.isEmpty(buildTasks)) return;

    CompilerManager compilerManager = CompilerManager.getInstance(project);
    CompileScope scope = createScope(compilerManager, context, Collections.emptySet(), false, false);
    // this will effectively run all before- and after- compilation tasks registered within CompilerManager
    EmptyCompileScopeBuildTask task = (EmptyCompileScopeBuildTask)buildTasks.iterator().next();
    if (task.isIncrementalBuild()) {
      compilerManager.make(scope, compileNotification);
    }
    else {
      compilerManager.compile(scope, compileNotification);
    }
  }

  private static void runModulesResourcesBuildTasks(@NotNull Project project,
                                                    @NotNull ProjectTaskContext context,
                                                    @NotNull CompileStatusNotification compileNotification,
                                                    @NotNull Map<Class<? extends ProjectTask>, List<ProjectTask>> tasksMap) {
    Collection<? extends ProjectTask> buildTasks = tasksMap.get(ModuleResourcesBuildTask.class);
    if (ContainerUtil.isEmpty(buildTasks)) return;

    CompilerManager compilerManager = CompilerManager.getInstance(project);

    ModulesBuildSettings modulesBuildSettings = assembleModulesBuildSettings(buildTasks);
    CompileScope scope = createScope(compilerManager, context,
                                     modulesBuildSettings.modules,
                                     modulesBuildSettings.includeDependentModules,
                                     modulesBuildSettings.includeRuntimeDependencies);
    List<String> moduleNames = ContainerUtil.map(modulesBuildSettings.modules, Module::getName);
    CompileScopeUtil.setResourcesScopeForExternalBuild(scope, moduleNames);

    if (modulesBuildSettings.isIncrementalBuild) {
      compilerManager.make(scope, compileNotification);
    }
    else {
      compilerManager.compile(scope, compileNotification);
    }
  }

  private static class ModulesBuildSettings {
    final boolean isIncrementalBuild;
    final boolean includeDependentModules;
    final boolean includeRuntimeDependencies;
    final Collection<? extends Module> modules;

    ModulesBuildSettings(boolean isIncrementalBuild,
                         boolean includeDependentModules,
                         boolean includeRuntimeDependencies,
                         Collection<? extends Module> modules) {
      this.isIncrementalBuild = isIncrementalBuild;
      this.includeDependentModules = includeDependentModules;
      this.includeRuntimeDependencies = includeRuntimeDependencies;
      this.modules = modules;
    }

    boolean isRebuild() {
      if (!isIncrementalBuild && !modules.isEmpty()) {
        final Module someModule = modules.iterator().next();
        final Module[] projectModules = ModuleManager.getInstance(someModule.getProject()).getModules();
        return projectModules.length == modules.size();
      }
      return false;
    }
  }

  private static ModulesBuildSettings assembleModulesBuildSettings(Collection<? extends ProjectTask> buildTasks) {
    Collection<Module> modules = new SmartList<>();
    Collection<ModuleBuildTask> incrementalTasks = ContainerUtil.newSmartList();
    Collection<ModuleBuildTask> excludeDependentTasks = ContainerUtil.newSmartList();
    Collection<ModuleBuildTask> excludeRuntimeTasks = ContainerUtil.newSmartList();

    for (ProjectTask buildProjectTask : buildTasks) {
      ModuleBuildTask moduleBuildTask = (ModuleBuildTask)buildProjectTask;
      modules.add(moduleBuildTask.getModule());

      if (moduleBuildTask.isIncrementalBuild()) {
        incrementalTasks.add(moduleBuildTask);
      }
      if (!moduleBuildTask.isIncludeDependentModules()) {
        excludeDependentTasks.add(moduleBuildTask);
      }
      if (!moduleBuildTask.isIncludeRuntimeDependencies()) {
        excludeRuntimeTasks.add(moduleBuildTask);
      }
    }

    boolean isIncrementalBuild = incrementalTasks.size() == buildTasks.size();
    boolean includeDependentModules = excludeDependentTasks.size() != buildTasks.size();
    boolean includeRuntimeDependencies = excludeRuntimeTasks.size() != buildTasks.size();

    if (!isIncrementalBuild && !incrementalTasks.isEmpty()) {
      assertModuleBuildSettingsConsistent(incrementalTasks, "will be built ignoring incremental build setting");
    }
    if (includeDependentModules && !excludeDependentTasks.isEmpty()) {
      assertModuleBuildSettingsConsistent(excludeDependentTasks, "will be built along with dependent modules");
    }
    if (includeRuntimeDependencies && !excludeRuntimeTasks.isEmpty()) {
      assertModuleBuildSettingsConsistent(excludeRuntimeTasks, "will be built along with runtime dependencies");
    }
    return new ModulesBuildSettings(isIncrementalBuild, includeDependentModules, includeRuntimeDependencies, modules);
  }

  private static void assertModuleBuildSettingsConsistent(Collection<? extends ModuleBuildTask> moduleBuildTasks, String warnMsg) {
    String moduleNames = StringUtil.join(moduleBuildTasks, task -> task.getModule().getName(), ", ");
    LOG.warn("Module" + (moduleBuildTasks.size() > 1 ? "s": "") + " : '" + moduleNames + "' " + warnMsg);
  }

  private static CompileScope createScope(CompilerManager compilerManager,
                                          ProjectTaskContext context,
                                          Collection<? extends Module> modules,
                                          boolean includeDependentModules,
                                          boolean includeRuntimeDependencies) {
    CompileScope scope = !modules.isEmpty()?
      compilerManager.createModulesCompileScope(modules.toArray(Module.EMPTY_ARRAY), includeDependentModules, includeRuntimeDependencies):
      new CompositeScope(CompileScope.EMPTY_ARRAY);

    if (context.isAutoRun()) {
      CompileDriver.setCompilationStartedAutomatically(scope);
    }

    RunConfiguration configuration = context.getRunConfiguration();
    if (configuration != null) {
      scope.putUserData(CompilerManager.RUN_CONFIGURATION_KEY, configuration);
      scope.putUserData(CompilerManager.RUN_CONFIGURATION_TYPE_ID_KEY, configuration.getType().getId());
    }
    ExecutionManagerImpl.EXECUTION_SESSION_ID_KEY.set(scope, context.getSessionId());
    return scope;
  }

  private static void runFilesBuildTasks(@NotNull Project project,
                                         @NotNull CompileStatusNotification compileNotification,
                                         @NotNull Map<Class<? extends ProjectTask>, List<ProjectTask>> tasksMap) {
    Collection<? extends ProjectTask> filesTargets = tasksMap.get(ModuleFilesBuildTask.class);
    if (!ContainerUtil.isEmpty(filesTargets)) {
      VirtualFile[] files = filesTargets.stream()
        .flatMap(target -> Stream.of(((ModuleFilesBuildTask)target).getFiles()))
        .toArray(VirtualFile[]::new);
      CompilerManager.getInstance(project).compile(files, compileNotification);
    }
  }

  private static void runArtifactsBuildTasks(@NotNull Project project,
                                             @NotNull ProjectTaskContext context,
                                             @NotNull CompileStatusNotification compileNotification,
                                             @NotNull Map<Class<? extends ProjectTask>, List<ProjectTask>> tasksMap) {

    Collection<? extends ProjectTask> buildTasks = tasksMap.get(ProjectModelBuildTask.class);
    if (!ContainerUtil.isEmpty(buildTasks)) {
      List<Artifact> toMake = new SmartList<>();
      List<Artifact> toCompile = new SmartList<>();
      for (ProjectTask buildProjectTask : buildTasks) {
        ProjectModelBuildTask buildTask = (ProjectModelBuildTask)buildProjectTask;
        ProjectModelBuildableElement buildableElement = buildTask.getBuildableElement();
        if (buildableElement instanceof Artifact) {
          if (buildTask.isIncrementalBuild()) {
            toMake.add((Artifact)buildableElement);
          }
          else {
            toCompile.add((Artifact)buildableElement);
          }
        }
      }

      buildArtifacts(project, toMake, context.getSessionId(), compileNotification, false);
      buildArtifacts(project, toCompile, context.getSessionId(), compileNotification, true);
    }
  }

  private static void buildArtifacts(@NotNull Project project,
                                     @NotNull List<? extends Artifact> artifacts,
                                     @Nullable Object sessionId,
                                     @Nullable CompileStatusNotification compileNotification,
                                     boolean forceArtifactBuild) {
    if (!artifacts.isEmpty()) {
      final CompileScope scope = ArtifactCompileScope.createArtifactsScope(project, artifacts, forceArtifactBuild);
      ArtifactsWorkspaceSettings.getInstance(project).setArtifactsToBuild(artifacts);
      ExecutionManagerImpl.EXECUTION_SESSION_ID_KEY.set(scope, sessionId);
      //in external build we can set 'rebuild' flag per target type
      CompilerManager.getInstance(project).make(scope, compileNotification);
    }
  }
}
