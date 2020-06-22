// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;
import gnu.trove.THashSet;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.Build;
import org.jetbrains.plugins.gradle.model.data.BuildParticipant;
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData;
import org.jetbrains.plugins.gradle.model.data.CompositeBuildData;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.*;
import static com.intellij.openapi.util.text.StringUtil.isEmpty;
import static com.intellij.openapi.util.text.StringUtil.isNotEmpty;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.BUILD_SRC_NAME;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.getDefaultModuleTypeId;

/**
 * @author Vladislav.Soroka
 */
public final class GradleBuildSrcProjectsResolver {
  public static final String BUILD_SRC_MODULE_PROPERTY = "buildSrcModule";
  @NotNull
  private final GradleProjectResolver myProjectResolver;
  @NotNull
  private final DefaultProjectResolverContext myResolverContext;
  @Nullable
  private final File myGradleUserHome;
  @Nullable
  private final GradleExecutionSettings myMainBuildExecutionSettings;
  @NotNull
  private final ExternalSystemTaskNotificationListener myListener;
  @NotNull
  private final ExternalSystemTaskId mySyncTaskId;
  @NotNull
  private final GradleProjectResolverExtension myResolverChain;

  public GradleBuildSrcProjectsResolver(@NotNull GradleProjectResolver projectResolver,
                                        @NotNull DefaultProjectResolverContext resolverContext,
                                        @Nullable File gradleUserHome,
                                        @Nullable GradleExecutionSettings mainBuildSettings,
                                        @NotNull ExternalSystemTaskNotificationListener listener,
                                        @NotNull ExternalSystemTaskId syncTaskId,
                                        @NotNull GradleProjectResolverExtension projectResolverChain) {
    myProjectResolver = projectResolver;
    myResolverContext = resolverContext;
    myGradleUserHome = gradleUserHome;
    myMainBuildExecutionSettings = mainBuildSettings;
    myListener = listener;
    mySyncTaskId = syncTaskId;
    myResolverChain = projectResolverChain;
  }

  public void discoverAndAppendTo(@NotNull DataNode<ProjectData> mainBuildProjectDataNode) {
    String gradleHome = myGradleUserHome == null ? null : myGradleUserHome.getPath();
    ProjectData mainBuildProjectData = mainBuildProjectDataNode.getData();
    String projectPath = mainBuildProjectData.getLinkedExternalProjectPath();

    Map<String, String> includedBuildsPaths = new HashMap<>();
    Map<String, String> buildNames = new HashMap<>();
    buildNames.put(projectPath, mainBuildProjectData.getExternalName());
    DataNode<CompositeBuildData> compositeBuildData = find(mainBuildProjectDataNode, CompositeBuildData.KEY);
    if (compositeBuildData != null) {
      for (BuildParticipant buildParticipant : compositeBuildData.getData().getCompositeParticipants()) {
        String buildParticipantRootPath = buildParticipant.getRootPath();
        buildNames.put(buildParticipantRootPath, buildParticipant.getRootProjectName());
        for (String path : buildParticipant.getProjects()) {
          includedBuildsPaths.put(path, buildParticipantRootPath);
        }
      }
    }

    MultiMap<String, DataNode<BuildScriptClasspathData>> buildClasspathNodesMap = new MultiMap<>();
    Map<String, ModuleData> includedModulesPaths = new HashMap<>();
    for (DataNode<ModuleData> moduleDataNode : findAll(mainBuildProjectDataNode, ProjectKeys.MODULE)) {
      String path = moduleDataNode.getData().getLinkedExternalProjectPath();
      includedModulesPaths.put(path, moduleDataNode.getData());
      DataNode<BuildScriptClasspathData> scriptClasspathDataNode = find(moduleDataNode, BuildScriptClasspathData.KEY);
      if (scriptClasspathDataNode != null) {
        String rootPath = includedBuildsPaths.get(path);
        buildClasspathNodesMap.putValue(rootPath != null ? rootPath : projectPath, scriptClasspathDataNode);
      }
    }

    List<String> jvmOptions = new SmartList<>();
    // the BuildEnvironment jvm arguments of the main build should be used for the 'buildSrc' import
    // to avoid spawning of the second gradle daemon
    BuildEnvironment mainBuildEnvironment = myResolverContext.getModels().getBuildEnvironment();
    if (mainBuildEnvironment != null) {
      jvmOptions.addAll(mainBuildEnvironment.getJava().getJvmArguments());
    }
    if (myMainBuildExecutionSettings != null) {
      jvmOptions.addAll(myMainBuildExecutionSettings.getJvmArguments());
    }

    Stream<Build> builds = new ToolingModelsProviderImpl(myResolverContext.getModels()).builds();
    builds.forEach(build -> {
      String buildPath = build.getBuildIdentifier().getRootDir().getPath();
      Collection<DataNode<BuildScriptClasspathData>> buildClasspathNodes = buildClasspathNodesMap.getModifiable(buildPath);

      GradleExecutionSettings buildSrcProjectSettings;
      if (gradleHome != null) {
        if (myMainBuildExecutionSettings != null) {
          buildSrcProjectSettings = new GradleExecutionSettings(gradleHome,
                                                                myMainBuildExecutionSettings.getServiceDirectory(),
                                                                DistributionType.LOCAL,
                                                                myMainBuildExecutionSettings.isOfflineWork());
          buildSrcProjectSettings.setIdeProjectPath(myMainBuildExecutionSettings.getIdeProjectPath());
          buildSrcProjectSettings.setJavaHome(myMainBuildExecutionSettings.getJavaHome());
          buildSrcProjectSettings.setResolveModulePerSourceSet(myMainBuildExecutionSettings.isResolveModulePerSourceSet());
          buildSrcProjectSettings.setUseQualifiedModuleNames(myMainBuildExecutionSettings.isUseQualifiedModuleNames());
          buildSrcProjectSettings.setRemoteProcessIdleTtlInMs(myMainBuildExecutionSettings.getRemoteProcessIdleTtlInMs());
          buildSrcProjectSettings.setVerboseProcessing(myMainBuildExecutionSettings.isVerboseProcessing());
          buildSrcProjectSettings.setWrapperPropertyFile(myMainBuildExecutionSettings.getWrapperPropertyFile());
          buildSrcProjectSettings.withArguments(myMainBuildExecutionSettings.getArguments())
            .withEnvironmentVariables(myMainBuildExecutionSettings.getEnv())
            .passParentEnvs(myMainBuildExecutionSettings.isPassParentEnvs())
            .withVmOptions(jvmOptions);
        }
        else {
          buildSrcProjectSettings = new GradleExecutionSettings(gradleHome, null, DistributionType.LOCAL, false);
        }
      }
      else {
        buildSrcProjectSettings = myMainBuildExecutionSettings;
      }

      final String buildSrcProjectPath = buildPath + "/buildSrc";
      DefaultProjectResolverContext buildSrcResolverCtx =
        new DefaultProjectResolverContext(mySyncTaskId, buildSrcProjectPath, buildSrcProjectSettings, myListener, myResolverContext.getPolicy(), false);
      myResolverContext.copyUserDataTo(buildSrcResolverCtx);
      String buildName = buildNames.get(buildPath);

      String buildSrcGroup = getBuildSrcGroup(buildPath, buildName);

      buildSrcResolverCtx.setBuildSrcGroup(buildSrcGroup);
      handleBuildSrcProject(mainBuildProjectDataNode,
                            buildName,
                            buildClasspathNodes,
                            includedModulesPaths,
                            buildSrcResolverCtx,
                            myProjectResolver.getProjectDataFunction(buildSrcResolverCtx, myResolverChain, true));
    });
  }

  private void handleBuildSrcProject(@NotNull DataNode<ProjectData> resultProjectDataNode,
                                     @Nullable String buildName,
                                     @NotNull Collection<DataNode<BuildScriptClasspathData>> buildClasspathNodes,
                                     @NotNull Map<String, ModuleData> includedModulesPaths,
                                     @NotNull DefaultProjectResolverContext buildSrcResolverCtx,
                                     @NotNull Function<ProjectConnection, DataNode<ProjectData>> projectConnectionDataNodeFunction) {
    final String projectPath = buildSrcResolverCtx.getProjectPath();
    File projectPathFile = new File(projectPath);
    if (!projectPathFile.isDirectory()) {
      return;
    }

    if (includedModulesPaths.containsKey(projectPath)) {
      // `buildSrc` has been already included into the main build (prohibited since 6.0, https://docs.gradle.org/current/userguide/upgrading_version_5.html#buildsrc_is_now_reserved_as_a_project_and_subproject_build_name)
      return;
    }

    if (ArrayUtil.isEmpty(projectPathFile.list((dir, name) -> !name.equals(".gradle") && !name.equals("build")))) {
      return;
    }

    if (buildSrcResolverCtx.isPreviewMode()) {
      ModuleData buildSrcModuleData =
        new ModuleData(":buildSrc", GradleConstants.SYSTEM_ID, getDefaultModuleTypeId(), BUILD_SRC_NAME, projectPath, projectPath);
      buildSrcModuleData.setProperty(BUILD_SRC_MODULE_PROPERTY, "true");
      resultProjectDataNode.createChild(ProjectKeys.MODULE, buildSrcModuleData);
      return;
    }

    final DataNode<ProjectData> buildSrcProjectDataNode = myProjectResolver.getHelper().execute(
      projectPath, buildSrcResolverCtx.getSettings(), mySyncTaskId, myListener, null, projectConnectionDataNodeFunction);

    if (buildSrcProjectDataNode == null) return;
    for (DataNode<LibraryData> libraryDataNode : getChildren(buildSrcProjectDataNode, ProjectKeys.LIBRARY)) {
      resultProjectDataNode.createChild(ProjectKeys.LIBRARY, libraryDataNode.getData());
    }

    Map<String, DataNode<? extends ModuleData>> buildSrcModules = new HashMap<>();

    boolean modulePerSourceSet = buildSrcResolverCtx.isResolveModulePerSourceSet();
    DataNode<? extends ModuleData> buildSrcModuleNode = null;
    for (DataNode<ModuleData> moduleNode : getChildren(buildSrcProjectDataNode, ProjectKeys.MODULE)) {
      final ModuleData moduleData = moduleNode.getData();
      buildSrcModules.put(moduleData.getId(), moduleNode);
      boolean isBuildSrcModule = BUILD_SRC_NAME.equals(moduleData.getExternalName());

      if (isBuildSrcModule && !modulePerSourceSet) {
        buildSrcModuleNode = moduleNode;
      }
      if (modulePerSourceSet) {
        for (DataNode<GradleSourceSetData> sourceSetNode : getChildren(moduleNode, GradleSourceSetData.KEY)) {
          buildSrcModules.put(sourceSetNode.getData().getId(), sourceSetNode);
          if (isBuildSrcModule && buildSrcModuleNode == null && sourceSetNode.getData().getExternalName().endsWith(":main")) {
            buildSrcModuleNode = sourceSetNode;
          }
        }
      }

      ModuleData includedModule = includedModulesPaths.get(moduleData.getLinkedExternalProjectPath());
      if (includedModule == null) {
        moduleData.setProperty(BUILD_SRC_MODULE_PROPERTY, "true");
        resultProjectDataNode.addChild(moduleNode);
        if (!buildSrcResolverCtx.isUseQualifiedModuleNames()) {
          // adjust ide module group
          if (moduleData.getIdeModuleGroup() != null) {
            String[] moduleGroup = ArrayUtil.prepend(
              isNotEmpty(buildName) ? buildName : resultProjectDataNode.getData().getInternalName(),
              moduleData.getIdeModuleGroup());
            moduleData.setIdeModuleGroup(moduleGroup);

            for (DataNode<GradleSourceSetData> sourceSetNode : getChildren(moduleNode, GradleSourceSetData.KEY)) {
              sourceSetNode.getData().setIdeModuleGroup(moduleGroup);
            }
          }
        }
      }
      else {
        includedModule.setProperty(BUILD_SRC_MODULE_PROPERTY, "true");
      }
    }
    if (buildSrcModuleNode != null) {
      Set<String> buildSrcRuntimeSourcesPaths = new THashSet<>();
      Set<String> buildSrcRuntimeClassesPaths = new THashSet<>();

      addSourcePaths(buildSrcRuntimeSourcesPaths, buildSrcModuleNode);

      for (DataNode<?> child : buildSrcModuleNode.getChildren()) {
        Object childData = child.getData();
        if (childData instanceof ModuleDependencyData && ((ModuleDependencyData)childData).getScope().isForProductionRuntime()) {
          DataNode<? extends ModuleData> depModuleNode = buildSrcModules.get(((ModuleDependencyData)childData).getTarget().getId());
          if (depModuleNode != null) {
            addSourcePaths(buildSrcRuntimeSourcesPaths, depModuleNode);
          }
        }
        else if (childData instanceof LibraryDependencyData) {
          LibraryDependencyData dependencyData = (LibraryDependencyData)childData;
          // exclude generated gradle-api jar the gradle api classes/sources handled separately by BuildClasspathModuleGradleDataService
          if (dependencyData.getExternalName().startsWith("gradle-api-")) {
            continue;
          }
          LibraryData libraryData = dependencyData.getTarget();
          buildSrcRuntimeSourcesPaths.addAll(libraryData.getPaths(LibraryPathType.SOURCE));
          buildSrcRuntimeClassesPaths.addAll(libraryData.getPaths(LibraryPathType.BINARY));
        }
      }

      if (!buildSrcRuntimeSourcesPaths.isEmpty() || !buildSrcRuntimeClassesPaths.isEmpty()) {
        buildClasspathNodes.forEach(classpathNode -> {
          BuildScriptClasspathData copyFrom = classpathNode.getData();

          List<BuildScriptClasspathData.ClasspathEntry> classpathEntries = new ArrayList<>(copyFrom.getClasspathEntries().size() + 1);
          classpathEntries.addAll(copyFrom.getClasspathEntries());
          classpathEntries.add(BuildScriptClasspathData.ClasspathEntry.create(
            new THashSet<>(buildSrcRuntimeClassesPaths),
            new THashSet<>(buildSrcRuntimeSourcesPaths),
            Collections.emptySet()
          ));

          BuildScriptClasspathData buildScriptClasspathData = new BuildScriptClasspathData(GradleConstants.SYSTEM_ID, classpathEntries);
          buildScriptClasspathData.setGradleHomeDir(copyFrom.getGradleHomeDir());

          DataNode<?> parent = classpathNode.getParent();
          assert parent != null;
          parent.createChild(BuildScriptClasspathData.KEY, buildScriptClasspathData);
          classpathNode.clear(true);
        });
      }
    }
  }

  private static void addSourcePaths(Set<String> paths, DataNode<? extends ModuleData> moduleNode) {
    getChildren(moduleNode, ProjectKeys.CONTENT_ROOT)
      .stream()
      .flatMap(contentNode -> contentNode.getData().getPaths(ExternalSystemSourceType.SOURCE).stream())
      .map(ContentRootData.SourceRoot::getPath)
      .forEach(paths::add);
  }

  @NotNull
  private static String getBuildSrcGroup(String buildPath, String buildName) {
    if (isEmpty(buildName)) {
      return new File(buildPath).getName();
    } else {
      return buildName;
    }
  }
}
