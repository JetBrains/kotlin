// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.importing.ProjectResolverPolicy;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.externalSystem.service.project.PerformanceTrace;
import com.intellij.openapi.externalSystem.util.ExternalSystemDebugEnvironment;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import gnu.trove.THashMap;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.ProjectModel;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.*;
import org.jetbrains.plugins.gradle.model.data.BuildParticipant;
import org.jetbrains.plugins.gradle.model.data.CompositeBuildData;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;
import org.jetbrains.plugins.gradle.remote.impl.GradleLibraryNamesMixer;
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleBuildParticipant;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.*;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.getDefaultModuleTypeId;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.getModuleId;

/**
 * @author Denis Zhdanov, Vladislav Soroka
 */
public class GradleProjectResolver implements ExternalSystemProjectResolver<GradleExecutionSettings> {

  private static final Logger LOG = Logger.getInstance(GradleProjectResolver.class);

  @NotNull private final GradleExecutionHelper myHelper;
  private final GradleLibraryNamesMixer myLibraryNamesMixer = new GradleLibraryNamesMixer();

  private final MultiMap<ExternalSystemTaskId, CancellationTokenSource> myCancellationMap = MultiMap.create();
  public static final Key<Map<String/* module id */, Pair<DataNode<GradleSourceSetData>, ExternalSourceSet>>> RESOLVED_SOURCE_SETS =
    Key.create("resolvedSourceSets");
  public static final Key<Map<String/* output path */, Pair<String /* module id*/, ExternalSystemSourceType>>> MODULES_OUTPUTS =
    Key.create("moduleOutputsMap");
  public static final Key<MultiMap<ExternalSystemSourceType, String /* output path*/>> GRADLE_OUTPUTS = Key.create("gradleOutputs");
  public static final Key<Map<String/* artifact path */, String /* module id*/>> CONFIGURATION_ARTIFACTS =
    Key.create("gradleArtifactsMap");

  private static final Key<File> GRADLE_HOME_DIR = Key.create("gradleHomeDir");

  // This constructor is called by external system API, see AbstractExternalSystemFacadeImpl class constructor.
  @SuppressWarnings("UnusedDeclaration")
  public GradleProjectResolver() {
    this(new GradleExecutionHelper());
  }

  public GradleProjectResolver(@NotNull GradleExecutionHelper helper) {
    myHelper = helper;
  }

  @Nullable
  @Override
  public DataNode<ProjectData> resolveProjectInfo(@NotNull final ExternalSystemTaskId syncTaskId,
                                                  @NotNull final String projectPath,
                                                  final boolean isPreviewMode,
                                                  @Nullable final GradleExecutionSettings settings,
                                                  @Nullable ProjectResolverPolicy resolverPolicy,
                                                  @NotNull final ExternalSystemTaskNotificationListener listener)
    throws ExternalSystemException, IllegalArgumentException, IllegalStateException {

    GradlePartialResolverPolicy gradleResolverPolicy = null;
    if (resolverPolicy != null) {
      if (resolverPolicy instanceof GradlePartialResolverPolicy) {
        gradleResolverPolicy = (GradlePartialResolverPolicy)resolverPolicy;
      }
      else {
        throw new ExternalSystemException("Unsupported project resolver policy: " + resolverPolicy.getClass().getName());
      }
    }
    if (isPreviewMode) {
      // Create project preview model w/o request to gradle, there are two main reasons for the it:
      // * Slow project open - even the simplest project info provided by gradle can be gathered too long (mostly because of new gradle distribution download and downloading build script dependencies)
      // * Ability to open  an invalid projects (e.g. with errors in build scripts)
      String projectName = new File(projectPath).getName();
      ProjectData projectData = new ProjectData(GradleConstants.SYSTEM_ID, projectName, projectPath, projectPath);
      DataNode<ProjectData> projectDataNode = new DataNode<>(ProjectKeys.PROJECT, projectData, null);

      final String ideProjectPath = settings == null ? null : settings.getIdeProjectPath();
      final String mainModuleFileDirectoryPath = ideProjectPath == null ? projectPath : ideProjectPath;

      projectDataNode
        .createChild(ProjectKeys.MODULE, new ModuleData(projectName, GradleConstants.SYSTEM_ID, getDefaultModuleTypeId(),
                                                        projectName, mainModuleFileDirectoryPath, projectPath))
        .createChild(ProjectKeys.CONTENT_ROOT, new ContentRootData(GradleConstants.SYSTEM_ID, projectPath));
      return projectDataNode;
    }

    DefaultProjectResolverContext resolverContext =
      new DefaultProjectResolverContext(syncTaskId, projectPath, settings, listener, gradleResolverPolicy, false);
    final CancellationTokenSource cancellationTokenSource = resolverContext.getCancellationTokenSource();
    myCancellationMap.putValue(resolverContext.getExternalSystemTaskId(), cancellationTokenSource);

    try {
      if (settings != null) {
        myHelper.ensureInstalledWrapper(syncTaskId, projectPath, settings, listener, cancellationTokenSource.token());
      }

      Predicate<GradleProjectResolverExtension> extensionsFilter =
        gradleResolverPolicy != null ? gradleResolverPolicy.getExtensionsFilter() : null;
      final GradleProjectResolverExtension projectResolverChain = createProjectResolverChain(resolverContext, extensionsFilter);
      final DataNode<ProjectData> projectDataNode = myHelper.execute(
        projectPath, settings, syncTaskId, listener, cancellationTokenSource,
        getProjectDataFunction(resolverContext, projectResolverChain, false));

      // auto-discover buildSrc projects of the main and included builds
      File gradleUserHome = resolverContext.getUserData(GRADLE_HOME_DIR);
      new GradleBuildSrcProjectsResolver(this, resolverContext, gradleUserHome, settings, listener, syncTaskId, projectResolverChain)
        .discoverAndAppendTo(projectDataNode);
      return projectDataNode;
    }
    finally {
      myCancellationMap.remove(resolverContext.getExternalSystemTaskId(), cancellationTokenSource);
    }
  }

  @NotNull
  Function<ProjectConnection, DataNode<ProjectData>> getProjectDataFunction(DefaultProjectResolverContext resolverContext,
                                                                            GradleProjectResolverExtension projectResolverChain,
                                                                            boolean isBuildSrcProject) {
    return new ProjectConnectionDataNodeFunction(resolverContext, projectResolverChain, isBuildSrcProject);
  }

  @NotNull
  GradleExecutionHelper getHelper() {
    return myHelper;
  }

  @Override
  public boolean cancelTask(@NotNull ExternalSystemTaskId id, @NotNull ExternalSystemTaskNotificationListener listener) {
    synchronized (myCancellationMap) {
      for (CancellationTokenSource cancellationTokenSource : myCancellationMap.get(id)) {
        cancellationTokenSource.cancel();
      }
    }
    return true;
  }

  @NotNull
  private DataNode<ProjectData> doResolveProjectInfo(@NotNull final DefaultProjectResolverContext resolverCtx,
                                                     @NotNull final GradleProjectResolverExtension projectResolverChain,
                                                     boolean isBuildSrcProject)
    throws IllegalArgumentException, IllegalStateException {
    final PerformanceTrace performanceTrace = new PerformanceTrace();
    final GradleProjectResolverExtension tracedResolverChain = new TracedProjectResolverExtension(projectResolverChain, performanceTrace);

    final BuildEnvironment buildEnvironment = GradleExecutionHelper.getBuildEnvironment(resolverCtx);
    GradleVersion gradleVersion = null;

    boolean isCompositeBuildsSupported = false;
    if (buildEnvironment != null) {
      gradleVersion = GradleVersion.version(buildEnvironment.getGradle().getGradleVersion());
      isCompositeBuildsSupported = gradleVersion.compareTo(GradleVersion.version("3.1")) >= 0;
      resolverCtx.setBuildEnvironment(buildEnvironment);
    }
    boolean useCustomSerialization = Registry.is("gradle.tooling.custom.serializer", true);
    final ProjectImportAction projectImportAction =
      new ProjectImportAction(resolverCtx.isPreviewMode(), isCompositeBuildsSupported, useCustomSerialization);

    GradleExecutionSettings executionSettings = resolverCtx.getSettings();
    if (executionSettings == null) {
      executionSettings = new GradleExecutionSettings(null, null, DistributionType.BUNDLED, false);
    }

    configureExecutionArgumentsAndVmOptions(executionSettings, resolverCtx, isBuildSrcProject);
    final Set<Class<?>> toolingExtensionClasses = new HashSet<>();
    for (GradleProjectResolverExtension resolverExtension = tracedResolverChain;
         resolverExtension != null;
         resolverExtension = resolverExtension.getNext()) {
      // inject ProjectResolverContext into gradle project resolver extensions
      resolverExtension.setProjectResolverContext(resolverCtx);
      // pre-import checks
      resolverExtension.preImportCheck();

      projectImportAction.addTargetTypes(resolverExtension.getTargetTypes());

      // register classes of extra gradle project models required for extensions (e.g. com.android.builder.model.AndroidProject)
      try {
        ProjectImportModelProvider modelProvider = resolverExtension.getModelProvider();
        if (modelProvider != null) {
          projectImportAction.addProjectImportModelProvider(modelProvider);
        }
        ProjectImportModelProvider projectsLoadedModelProvider = resolverExtension.getProjectsLoadedModelProvider();
        if (projectsLoadedModelProvider != null) {
          projectImportAction.addProjectImportModelProvider(projectsLoadedModelProvider, true);
        }
      }
      catch (Throwable t) {
        LOG.warn(t);
      }

      // collect tooling extensions classes
      try {
        toolingExtensionClasses.addAll(resolverExtension.getToolingExtensionsClasses());
      }
      catch (Throwable t) {
        LOG.warn(t);
      }
    }
    File initScript = GradleExecutionHelper.generateInitScript(isBuildSrcProject, toolingExtensionClasses);
    if (initScript != null) {
      executionSettings.withArguments(GradleConstants.INIT_SCRIPT_CMD_OPTION, initScript.getAbsolutePath());
    }

    BuildActionRunner buildActionRunner = new BuildActionRunner(resolverCtx, projectImportAction, executionSettings, myHelper);
    resolverCtx.checkCancelled();

    final long startTime = System.currentTimeMillis();
    ProjectImportAction.AllModels allModels;
    CountDownLatch buildFinishWaiter = new CountDownLatch(1);
    try {
      allModels = buildActionRunner.fetchModels(
        models -> {
          for (GradleProjectResolverExtension resolver = tracedResolverChain; resolver != null; resolver = resolver.getNext()) {
            resolver.projectsLoaded(models);
          }
        },
        (exception) -> {
          try {
            for (GradleProjectResolverExtension resolver = tracedResolverChain; resolver != null; resolver = resolver.getNext()) {
              resolver.buildFinished(exception);
            }
          }
          finally {
            buildFinishWaiter.countDown();
          }
        });
      performanceTrace.addTrace(allModels.getPerformanceTrace());
    }
    catch (Exception e) {
      buildFinishWaiter.countDown();
      throw e;
    }
    finally {
      ProgressIndicatorUtils.awaitWithCheckCanceled(buildFinishWaiter);
      final long timeInMs = (System.currentTimeMillis() - startTime);
      performanceTrace.logPerformance("Gradle data obtained", timeInMs);
      LOG.debug(String.format("Gradle data obtained in %d ms", timeInMs));
    }

    resolverCtx.checkCancelled();
    if (useCustomSerialization) {
      assert gradleVersion != null;
      allModels.initToolingSerializer(gradleVersion);
    }

    allModels.setBuildEnvironment(buildEnvironment);

    final long startDataConversionTime = System.currentTimeMillis();
    extractExternalProjectModels(allModels, resolverCtx, useCustomSerialization);

    String projectName = allModels.getMainBuild().getName();
    ModifiableGradleProjectModelImpl modifiableGradleProjectModel = new ModifiableGradleProjectModelImpl(projectName, resolverCtx.getProjectPath());
    ToolingModelsProvider modelsProvider = new ToolingModelsProviderImpl(allModels);
    ProjectModelContributor.EP_NAME.forEachExtensionSafe(extension -> {
      resolverCtx.checkCancelled();
      final long starResolveTime = System.currentTimeMillis();
      extension.accept(modifiableGradleProjectModel, modelsProvider, resolverCtx);
      final long resolveTimeInMs = (System.currentTimeMillis() - starResolveTime);
      performanceTrace.logPerformance("Project model contributed by " + extension.getClass().getSimpleName(), resolveTimeInMs);
      LOG.debug(String.format("Project model contributed by `" + extension.getClass().getSimpleName() + "` in %d ms", resolveTimeInMs));
    });

    DataNode<ProjectData> projectDataNode = modifiableGradleProjectModel.buildDataNodeGraph();
    DataNode<PerformanceTrace> performanceTraceNode = new DataNode<>(PerformanceTrace.TRACE_NODE_KEY, performanceTrace, projectDataNode);
    projectDataNode.addChild(performanceTraceNode);

    Set<? extends IdeaModule> gradleModules = Collections.emptySet();
    IdeaProject ideaProject = allModels.getModel(IdeaProject.class);
    if (ideaProject != null) {
      tracedResolverChain.populateProjectExtraModels(ideaProject, projectDataNode);
      gradleModules = ideaProject.getModules();
      if (gradleModules == null || gradleModules.isEmpty()) {
        throw new IllegalStateException("No modules found for the target project: " + ideaProject);
      }
    }

    Collection<IdeaModule> includedModules = exposeCompositeBuild(allModels, resolverCtx, projectDataNode);
    final Map<String /* module id */, Pair<DataNode<ModuleData>, IdeaModule>> moduleMap = new HashMap<>();
    final Map<String /* module id */, Pair<DataNode<GradleSourceSetData>, ExternalSourceSet>> sourceSetsMap = new HashMap<>();
    projectDataNode.putUserData(RESOLVED_SOURCE_SETS, sourceSetsMap);

    final Map<String/* output path */, Pair<String /* module id*/, ExternalSystemSourceType>> moduleOutputsMap =
      new THashMap<>(FileUtil.PATH_HASHING_STRATEGY);
    projectDataNode.putUserData(MODULES_OUTPUTS, moduleOutputsMap);
    final Map<String/* artifact path */, String /* module id*/> artifactsMap =
      new THashMap<>(FileUtil.PATH_HASHING_STRATEGY);
    projectDataNode.putUserData(CONFIGURATION_ARTIFACTS, artifactsMap);

    // import modules data
    for (IdeaModule gradleModule : ContainerUtil.concat(gradleModules, includedModules)) {
      if (gradleModule == null) {
        continue;
      }

      resolverCtx.checkCancelled();

      if (ExternalSystemDebugEnvironment.DEBUG_ORPHAN_MODULES_PROCESSING) {
        LOG.info(String.format("Importing module data: %s", gradleModule));
      }
      final String moduleName = gradleModule.getName();
      if (moduleName == null) {
        throw new IllegalStateException("Module with undefined name detected: " + gradleModule);
      }

      DataNode<ModuleData> moduleDataNode = tracedResolverChain.createModule(gradleModule, projectDataNode);
      if (moduleDataNode == null) continue;
      String mainModuleId = getModuleId(resolverCtx, gradleModule);

      if (moduleMap.containsKey(mainModuleId)) {
        // we should ensure deduplicated module names in the scope of single import
        throw new IllegalStateException("Duplicate modules names detected: " + gradleModule);
      }
      moduleMap.put(mainModuleId, Pair.create(moduleDataNode, gradleModule));
    }

    executionSettings.getExecutionWorkspace().setModuleIdIndex(moduleMap);

    File gradleHomeDir = null;
    // populate modules nodes
    for (final Pair<DataNode<ModuleData>, IdeaModule> pair : moduleMap.values()) {
      final DataNode<ModuleData> moduleDataNode = pair.first;
      final IdeaModule ideaModule = pair.second;

      if (gradleHomeDir == null) {
        final BuildScriptClasspathModel buildScriptClasspathModel =
          resolverCtx.getExtraProject(ideaModule, BuildScriptClasspathModel.class);
        if (buildScriptClasspathModel != null) {
          gradleHomeDir = buildScriptClasspathModel.getGradleHomeDir();
        }
      }

      tracedResolverChain.populateModuleContentRoots(ideaModule, moduleDataNode);
      tracedResolverChain.populateModuleCompileOutputSettings(ideaModule, moduleDataNode);
      if (!isBuildSrcProject) {
        tracedResolverChain.populateModuleTasks(ideaModule, moduleDataNode, projectDataNode);
      }

      final List<DataNode<? extends ModuleData>> modules = new SmartList<>();
      modules.add(moduleDataNode);
      modules.addAll(findAll(moduleDataNode, GradleSourceSetData.KEY));

      final ExternalSystemSourceType[] sourceTypes = new ExternalSystemSourceType[]{
        ExternalSystemSourceType.SOURCE,
        ExternalSystemSourceType.RESOURCE,
        ExternalSystemSourceType.TEST,
        ExternalSystemSourceType.TEST_RESOURCE
      };
      for (DataNode<? extends ModuleData> module : modules) {
        final ModuleData moduleData = module.getData();
        for (ExternalSystemSourceType sourceType : sourceTypes) {
          final String path = moduleData.getCompileOutputPath(sourceType);
          if (path != null) {
            moduleOutputsMap.put(path, Pair.create(moduleData.getId(), sourceType));
          }
        }

        if (moduleData instanceof GradleSourceSetData) {
          for (File artifactFile : moduleData.getArtifacts()) {
            artifactsMap.put(toCanonicalPath(artifactFile.getAbsolutePath()), moduleData.getId());
          }
        }
      }
    }
    resolverCtx.putUserData(GRADLE_HOME_DIR, gradleHomeDir);

    for (final Pair<DataNode<ModuleData>, IdeaModule> pair : moduleMap.values()) {
      final DataNode<ModuleData> moduleDataNode = pair.first;
      final IdeaModule ideaModule = pair.second;
      tracedResolverChain.populateModuleDependencies(ideaModule, moduleDataNode, projectDataNode);
      tracedResolverChain.populateModuleExtraModels(ideaModule, moduleDataNode);
    }
    mergeSourceSetContentRoots(moduleMap, resolverCtx);
    if (resolverCtx.isResolveModulePerSourceSet()) {
      mergeLibraryAndModuleDependencyData(resolverCtx, projectDataNode, resolverCtx.getGradleUserHome(), gradleHomeDir, gradleVersion);
    }

    for (GradleProjectResolverExtension resolver = tracedResolverChain; resolver != null; resolver = resolver.getNext()) {
      resolver.resolveFinished(projectDataNode);
    }

    projectDataNode.putUserData(RESOLVED_SOURCE_SETS, null);
    projectDataNode.putUserData(MODULES_OUTPUTS, null);
    projectDataNode.putUserData(CONFIGURATION_ARTIFACTS, null);

    // ensure unique library names
    Collection<DataNode<LibraryData>> libraries = getChildren(projectDataNode, ProjectKeys.LIBRARY);
    myLibraryNamesMixer.mixNames(libraries);

    final long timeConversionInMs = (System.currentTimeMillis() - startDataConversionTime);
    performanceTrace.logPerformance("Gradle project data processed", timeConversionInMs);
    LOG.debug(String.format("Project data resolved in %d ms", timeConversionInMs));
    return projectDataNode;
  }

  private static void configureExecutionArgumentsAndVmOptions(@NotNull GradleExecutionSettings executionSettings,
                                                              @NotNull DefaultProjectResolverContext resolverCtx,
                                                              boolean isBuildSrcProject) {
    executionSettings.withArgument("-Didea.sync.active=true");
    if (resolverCtx.isResolveModulePerSourceSet()) {
      executionSettings.withArgument("-Didea.resolveSourceSetDependencies=true");
    }
    if (!isBuildSrcProject) {
      for (GradleBuildParticipant buildParticipant : executionSettings.getExecutionWorkspace().getBuildParticipants()) {
        executionSettings.withArguments(GradleConstants.INCLUDE_BUILD_CMD_OPTION, buildParticipant.getProjectPath());
      }
    }

    GradleImportCustomizer importCustomizer = GradleImportCustomizer.get();
    GradleProjectResolverUtil.createProjectResolvers(resolverCtx).forEachOrdered(extension -> {
      if (importCustomizer == null || importCustomizer.useExtraJvmArgs()) {
        // collect extra JVM arguments provided by gradle project resolver extensions
        ParametersList parametersList = new ParametersList();
        for (Pair<String, String> jvmArg : extension.getExtraJvmArgs()) {
          parametersList.addProperty(jvmArg.first, jvmArg.second);
        }
        executionSettings.withVmOptions(parametersList.getParameters());
      }
      // collect extra command-line arguments
      executionSettings.withArguments(extension.getExtraCommandLineArgs());
    });
  }

  @NotNull
  private static Collection<IdeaModule> exposeCompositeBuild(ProjectImportAction.AllModels allModels,
                                                             DefaultProjectResolverContext resolverCtx,
                                                             DataNode<ProjectData> projectDataNode) {
    if (resolverCtx.getSettings() != null && !resolverCtx.getSettings().getExecutionWorkspace().getBuildParticipants().isEmpty()) {
      return Collections.emptyList();
    }
    CompositeBuildData compositeBuildData;
    List<IdeaModule> gradleIncludedModules = new SmartList<>();
    List<Build> includedBuilds = allModels.getIncludedBuilds();
    if (!includedBuilds.isEmpty()) {
      ProjectData projectData = projectDataNode.getData();
      compositeBuildData = new CompositeBuildData(projectData.getLinkedExternalProjectPath());
      for (Build build : includedBuilds) {
        if (!build.getProjects().isEmpty()) {
          IdeaProject ideaProject = allModels.getModel(build, IdeaProject.class);
          assert ideaProject != null;
          String rootProjectName = build.getName();
          BuildParticipant buildParticipant = new BuildParticipant();
          gradleIncludedModules.addAll(ideaProject.getModules());
          try {
            String projectPath = toCanonicalPath(build.getBuildIdentifier().getRootDir().getCanonicalPath());
            buildParticipant.setRootProjectName(rootProjectName);
            buildParticipant.setRootPath(projectPath);
            for (IdeaModule module : ideaProject.getModules()) {
              try {
                String modulePath = toCanonicalPath(module.getGradleProject().getProjectDirectory().getCanonicalPath());
                buildParticipant.getProjects().add(modulePath);
              }
              catch (IOException e) {
                LOG.warn("construction of the canonical path for the module fails", e);
              }
            }
            compositeBuildData.getCompositeParticipants().add(buildParticipant);
          }
          catch (IOException e) {
            LOG.warn("construction of the canonical path for the module fails", e);
          }
        }
      }
      projectDataNode.createChild(CompositeBuildData.KEY, compositeBuildData);
    }
    return gradleIncludedModules;
  }

  private static void mergeLibraryAndModuleDependencyData(@NotNull ProjectResolverContext context,
                                                          @NotNull DataNode<ProjectData> projectDataNode,
                                                          @NotNull File gradleUserHomeDir,
                                                          @Nullable File gradleHomeDir,
                                                          @Nullable GradleVersion gradleVersion) {
    final Map<String, Pair<DataNode<GradleSourceSetData>, ExternalSourceSet>> sourceSetMap =
      projectDataNode.getUserData(RESOLVED_SOURCE_SETS);
    assert sourceSetMap != null;

    final Map<String, Pair<String, ExternalSystemSourceType>> moduleOutputsMap =
      projectDataNode.getUserData(MODULES_OUTPUTS);
    assert moduleOutputsMap != null;

    final Map<String, String> artifactsMap = projectDataNode.getUserData(CONFIGURATION_ARTIFACTS);
    assert artifactsMap != null;

    final Collection<DataNode<LibraryDependencyData>> libraryDependencies =
      findAllRecursively(projectDataNode, ProjectKeys.LIBRARY_DEPENDENCY);

    LibraryDataNodeSubstitutor librarySubstitutor =
      new LibraryDataNodeSubstitutor(context, gradleUserHomeDir, gradleHomeDir, gradleVersion, sourceSetMap, moduleOutputsMap, artifactsMap);
    for (DataNode<LibraryDependencyData> libraryDependencyDataNode : libraryDependencies) {
      librarySubstitutor.run(libraryDependencyDataNode);
    }
  }

  private static void extractExternalProjectModels(@NotNull ProjectImportAction.AllModels models,
                                                   @NotNull ProjectResolverContext resolverCtx,
                                                   boolean useCustomSerialization) {
    resolverCtx.setModels(models);
    final Class<? extends ExternalProject> modelClazz = resolverCtx.isPreviewMode() ? ExternalProjectPreview.class : ExternalProject.class;
    final ExternalProject externalRootProject = models.getModel(modelClazz);
    if (externalRootProject == null) return;

    final DefaultExternalProject wrappedExternalRootProject =
      useCustomSerialization ? (DefaultExternalProject)externalRootProject : new DefaultExternalProject(externalRootProject);
    models.addModel(wrappedExternalRootProject, ExternalProject.class);
    final Map<String, DefaultExternalProject> externalProjectsMap = createExternalProjectsMap(wrappedExternalRootProject);

    Collection<Project> projects = models.getMainBuild().getProjects();
    for (Project project : projects) {
      ExternalProject externalProject = externalProjectsMap.get(project.getProjectIdentifier().getProjectPath());
      if (externalProject != null) {
        models.addModel(externalProject, ExternalProject.class, project);
      }
    }

    for (Build includedBuild : models.getIncludedBuilds()) {
      final ExternalProject externalIncludedRootProject = models.getModel(includedBuild, modelClazz);
      if (externalIncludedRootProject == null) continue;
      final DefaultExternalProject wrappedExternalIncludedRootProject = useCustomSerialization
                                                                        ? (DefaultExternalProject)externalIncludedRootProject
                                                                        : new DefaultExternalProject(externalIncludedRootProject);
      wrappedExternalRootProject.getChildProjects().put(wrappedExternalIncludedRootProject.getName(), wrappedExternalIncludedRootProject);
      final Map<String, DefaultExternalProject> externalIncludedProjectsMap = createExternalProjectsMap(wrappedExternalIncludedRootProject);
      for (ProjectModel project : includedBuild.getProjects()) {
        ExternalProject externalProject = externalIncludedProjectsMap.get(project.getProjectIdentifier().getProjectPath());
        if (externalProject != null) {
          models.addModel(externalProject, ExternalProject.class, project);
        }
      }
    }
  }

  @NotNull
  private static Map<String, DefaultExternalProject> createExternalProjectsMap(@Nullable DefaultExternalProject rootExternalProject) {
    final Map<String, DefaultExternalProject> externalProjectMap = new THashMap<>();
    if (rootExternalProject == null) return externalProjectMap;
    ArrayDeque<DefaultExternalProject> queue = new ArrayDeque<>();
    queue.add(rootExternalProject);
    DefaultExternalProject externalProject;
    while ((externalProject = queue.pollFirst()) != null) {
      queue.addAll(externalProject.getChildProjects().values());
      externalProjectMap.put(externalProject.getQName(), externalProject);
    }
    return externalProjectMap;
  }

  private static class Counter {
    int count;
    void increment() {
      count++;
    }

    @Override
    public String toString() {
      return String.valueOf(count);
    }
  }

  private static void mergeSourceSetContentRoots(@NotNull Map<String, Pair<DataNode<ModuleData>, IdeaModule>> moduleMap,
                                                 @NotNull ProjectResolverContext resolverCtx) {
    final Factory<Counter> counterFactory = () -> new Counter();

    final Map<String, Counter> weightMap = new HashMap<>();
    for (final Pair<DataNode<ModuleData>, IdeaModule> pair : moduleMap.values()) {
      final DataNode<ModuleData> moduleNode = pair.first;
      for (DataNode<ContentRootData> contentRootNode : findAll(moduleNode, ProjectKeys.CONTENT_ROOT)) {
        File file = new File(contentRootNode.getData().getRootPath());
        while (file != null) {
          ContainerUtil.getOrCreate(weightMap, file.getPath(), counterFactory).increment();
          file = file.getParentFile();
        }
      }

      for (DataNode<GradleSourceSetData> sourceSetNode : findAll(moduleNode, GradleSourceSetData.KEY)) {
        final Set<String> set = new HashSet<>();
        for (DataNode<ContentRootData> contentRootNode : findAll(sourceSetNode, ProjectKeys.CONTENT_ROOT)) {
          File file = new File(contentRootNode.getData().getRootPath());
          while (file != null) {
            set.add(file.getPath());
            file = file.getParentFile();
          }
        }
        for (String path : set) {
          ContainerUtil.getOrCreate(weightMap, path, counterFactory).increment();
        }
      }
    }
    for (final Pair<DataNode<ModuleData>, IdeaModule> pair : moduleMap.values()) {
      final DataNode<ModuleData> moduleNode = pair.first;
      final ExternalProject externalProject = resolverCtx.getExtraProject(pair.second, ExternalProject.class);
      if (externalProject == null) continue;

      if (resolverCtx.isResolveModulePerSourceSet()) {
        for (DataNode<GradleSourceSetData> sourceSetNode : findAll(moduleNode, GradleSourceSetData.KEY)) {
          mergeModuleContentRoots(weightMap, externalProject, sourceSetNode);
        }
      }
      else {
        mergeModuleContentRoots(weightMap, externalProject, moduleNode);
      }
    }
  }

  private static void mergeModuleContentRoots(@NotNull Map<String, Counter> weightMap,
                                              @NotNull ExternalProject externalProject,
                                              @NotNull DataNode<? extends ModuleData> moduleNode) {
    final File buildDir = externalProject.getBuildDir();
    final MultiMap<String, ContentRootData> sourceSetRoots = MultiMap.create();
    Collection<DataNode<ContentRootData>> contentRootNodes = findAll(moduleNode, ProjectKeys.CONTENT_ROOT);
    if(contentRootNodes.size() <= 1) return;

    for (DataNode<ContentRootData> contentRootNode : contentRootNodes) {
      File root = new File(contentRootNode.getData().getRootPath());
      if (FileUtil.isAncestor(buildDir, root, true)) continue;

      while (weightMap.containsKey(root.getParent()) && weightMap.get(root.getParent()).count <= 1) {
        root = root.getParentFile();
      }

      ContentRootData mergedContentRoot = null;
      String rootPath = toCanonicalPath(root.getAbsolutePath());
      Set<String> paths = new HashSet<>(sourceSetRoots.keySet());
      for (String path : paths) {
        if (FileUtil.isAncestor(rootPath, path, true)) {
          Collection<ContentRootData> values = sourceSetRoots.remove(path);
          if (values != null) {
            sourceSetRoots.putValues(rootPath, values);
          }
        }
        else if (FileUtil.isAncestor(path, rootPath, false)) {
          Collection<ContentRootData> contentRoots = sourceSetRoots.get(path);
          for (ContentRootData rootData : contentRoots) {
            if (StringUtil.equals(rootData.getRootPath(), path)) {
              mergedContentRoot = rootData;
              break;
            }
          }
          if (mergedContentRoot == null) {
            mergedContentRoot = contentRoots.iterator().next();
          }
          break;
        }
        if(sourceSetRoots.size() == 1) break;
      }

      if (mergedContentRoot == null) {
        mergedContentRoot = new ContentRootData(GradleConstants.SYSTEM_ID, root.getAbsolutePath());
        sourceSetRoots.putValue(mergedContentRoot.getRootPath(), mergedContentRoot);
      }

      for (ExternalSystemSourceType sourceType : ExternalSystemSourceType.values()) {
        for (ContentRootData.SourceRoot sourceRoot : contentRootNode.getData().getPaths(sourceType)) {
          mergedContentRoot.storePath(sourceType, sourceRoot.getPath(), sourceRoot.getPackagePrefix());
        }
      }

      contentRootNode.clear(true);
    }

    for (Map.Entry<String, Collection<ContentRootData>> entry : sourceSetRoots.entrySet()) {
      final String rootPath = entry.getKey();
      final ContentRootData ideContentRoot = new ContentRootData(GradleConstants.SYSTEM_ID, rootPath);

      for (ContentRootData rootData : entry.getValue()) {
        for (ExternalSystemSourceType sourceType : ExternalSystemSourceType.values()) {
          Collection<ContentRootData.SourceRoot> roots = rootData.getPaths(sourceType);
          for (ContentRootData.SourceRoot sourceRoot : roots) {
            ideContentRoot.storePath(sourceType, sourceRoot.getPath(), sourceRoot.getPackagePrefix());
          }
        }
      }

      moduleNode.createChild(ProjectKeys.CONTENT_ROOT, ideContentRoot);
    }
  }

  private class ProjectConnectionDataNodeFunction implements Function<ProjectConnection, DataNode<ProjectData>> {
    @NotNull private final GradleProjectResolverExtension myProjectResolverChain;
    private final boolean myIsBuildSrcProject;
    private final DefaultProjectResolverContext myResolverContext;

    private ProjectConnectionDataNodeFunction(@NotNull DefaultProjectResolverContext resolverContext,
                                              @NotNull GradleProjectResolverExtension projectResolverChain, boolean isBuildSrcProject) {
      myResolverContext = resolverContext;
      myProjectResolverChain = projectResolverChain;
      myIsBuildSrcProject = isBuildSrcProject;
    }

    @Override
    public DataNode<ProjectData> fun(ProjectConnection connection) {
      try {
        myCancellationMap.putValue(myResolverContext.getExternalSystemTaskId(), myResolverContext.getCancellationTokenSource());
        myResolverContext.setConnection(connection);
        return doResolveProjectInfo(myResolverContext, myProjectResolverChain, myIsBuildSrcProject);
      }
      catch (RuntimeException e) {
        LOG.info("Gradle project resolve error", e);
        ExternalSystemException esException = ExceptionUtil.findCause(e, ExternalSystemException.class);
        if (esException != null && esException != e) {
          LOG.info("\nCaused by: " + esException.getOriginalReason());
        }
        throw myProjectResolverChain.getUserFriendlyError(
          myResolverContext.getBuildEnvironment(), e, myResolverContext.getProjectPath(), null);
      }
      finally {
        myCancellationMap.remove(myResolverContext.getExternalSystemTaskId(), myResolverContext.getCancellationTokenSource());
      }
    }
  }

  @ApiStatus.Experimental // chaining of resolver extensions complicates things and can be replaced in future
  public static GradleProjectResolverExtension createProjectResolverChain() {
    return createProjectResolverChain(null, null);
  }

  @NotNull
  private static GradleProjectResolverExtension createProjectResolverChain(@Nullable DefaultProjectResolverContext resolverContext,
                                                                           @Nullable Predicate<? super GradleProjectResolverExtension> extensionsFilter) {
    Stream<GradleProjectResolverExtension> extensions = GradleProjectResolverUtil.createProjectResolvers(resolverContext);
    if (extensionsFilter != null) {
      extensions = extensions.filter(extensionsFilter.or(BaseResolverExtension.class::isInstance));
    }

    Deque<GradleProjectResolverExtension> deque = new ArrayDeque<>();
    extensions.forEachOrdered(extension -> {
      final GradleProjectResolverExtension previous = deque.peekLast();
      if (previous != null) {
        previous.setNext(extension);
        if (previous.getNext() != extension) {
          throw new AssertionError("Illegal next resolver got, current resolver class is " + previous.getClass().getName());
        }
      }
      deque.add(extension);
    });

    GradleProjectResolverExtension firstResolver = deque.peekFirst();
    GradleProjectResolverExtension resolverExtension = firstResolver;
    assert resolverExtension != null;
    while (resolverExtension.getNext() != null) {
      resolverExtension = resolverExtension.getNext();
    }
    if (!(resolverExtension instanceof BaseResolverExtension)) {
      throw new AssertionError("Illegal last resolver got of class " + resolverExtension.getClass().getName());
    }

    GradleProjectResolverExtension chainWrapper = new AbstractProjectResolverExtension() {
      @NotNull
      @Override
      public ExternalSystemException getUserFriendlyError(@Nullable BuildEnvironment buildEnvironment,
                                                          @NotNull Throwable error,
                                                          @NotNull String projectPath,
                                                          @Nullable String buildFilePath) {
        ExternalSystemException friendlyError = super.getUserFriendlyError(buildEnvironment, error, projectPath, buildFilePath);
        return new BaseProjectImportErrorHandler()
          .checkErrorsWithoutQuickFixes(buildEnvironment, error, projectPath, buildFilePath, friendlyError);
      }
    };
    chainWrapper.setNext(firstResolver);
    return chainWrapper;
  }
}
