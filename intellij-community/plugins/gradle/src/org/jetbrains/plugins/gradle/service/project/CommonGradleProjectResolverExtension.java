// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.build.events.MessageEvent;
import com.intellij.build.issue.BuildIssue;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.debugger.DebuggerBackendExtension;
import com.intellij.openapi.externalSystem.model.ConfigurationDataImpl;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager;
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.externalSystem.service.notification.NotificationSource;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.externalSystem.util.PathPrefixTreeMap;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.roots.ui.configuration.SdkLookupDecision;
import com.intellij.openapi.roots.ui.configuration.SdkLookupUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.MultiMap;
import gnu.trove.THashSet;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.UnsupportedMethodException;
import org.gradle.tooling.model.idea.*;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.issue.UnresolvedDependencySyncIssue;
import org.jetbrains.plugins.gradle.model.*;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;
import org.jetbrains.plugins.gradle.model.tests.ExternalTestSourceMapping;
import org.jetbrains.plugins.gradle.model.tests.ExternalTestsModel;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;
import org.jetbrains.plugins.gradle.service.project.data.GradleExtensionsDataService;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.intellij.openapi.util.text.StringUtil.*;
import static com.intellij.util.containers.ContainerUtil.newLinkedHashSet;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolver.CONFIGURATION_ARTIFACTS;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolver.MODULES_OUTPUTS;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.*;

/**
 * {@link CommonGradleProjectResolverExtension} provides implementation of Gradle project resolver common to all project types.
 *
 * @author Vladislav.Soroka
 */
@Order(Integer.MAX_VALUE - 1)
public class CommonGradleProjectResolverExtension extends AbstractProjectResolverExtension {
  private static final Logger LOG = Logger.getInstance(CommonGradleProjectResolverExtension.class);

  @NotNull @NonNls private static final String UNRESOLVED_DEPENDENCY_PREFIX = "unresolved dependency - ";

  @Override
  public void populateProjectExtraModels(@NotNull IdeaProject gradleProject, @NotNull DataNode<ProjectData> ideProject) {
    final ExternalProject externalProject = resolverCtx.getExtraProject(ExternalProject.class);
    if (externalProject != null) {
      ideProject.createChild(ExternalProjectDataCache.KEY, externalProject);
      ideProject.getData().setDescription(externalProject.getDescription());
    }

    final IntelliJSettings intellijSettings = resolverCtx.getExtraProject(IntelliJProjectSettings.class);
    if (intellijSettings != null) {
      ideProject.createChild(ProjectKeys.CONFIGURATION,
                             new ConfigurationDataImpl(GradleConstants.SYSTEM_ID, intellijSettings.getSettings()));
    }

    populateProjectSdkModel(gradleProject, ideProject);
  }

  @NotNull
  @Override
  @SuppressWarnings("deprecation")
  public DataNode<ModuleData> createModule(@NotNull IdeaModule gradleModule, @NotNull DataNode<ProjectData> projectDataNode) {
    DataNode<ModuleData> mainModuleNode = createMainModule(resolverCtx, gradleModule, projectDataNode);
    final ModuleData mainModuleData = mainModuleNode.getData();
    final String mainModuleConfigPath = mainModuleData.getLinkedExternalProjectPath();
    final String mainModuleFileDirectoryPath = mainModuleData.getModuleFileDirectoryPath();
    final String jdkName = getJdkName(gradleModule);

    String[] moduleGroup = null;
    if (!resolverCtx.isUseQualifiedModuleNames()) {
      moduleGroup = getIdeModuleGroup(mainModuleData.getInternalName(), gradleModule);
      mainModuleData.setIdeModuleGroup(moduleGroup);
    }

    if (resolverCtx.isResolveModulePerSourceSet()) {
      ExternalProject externalProject = getExternalProject(gradleModule, resolverCtx);
      assert externalProject != null;
      for (ExternalSourceSet sourceSet : externalProject.getSourceSets().values()) {
        final String moduleId = getModuleId(resolverCtx, gradleModule, sourceSet);
        final String moduleExternalName = gradleModule.getName() + ":" + sourceSet.getName();
        final String moduleInternalName = getInternalModuleName(gradleModule, externalProject, sourceSet.getName(), resolverCtx);

        GradleSourceSetData sourceSetData = new GradleSourceSetData(
          moduleId, moduleExternalName, moduleInternalName, mainModuleFileDirectoryPath, mainModuleConfigPath);

        sourceSetData.setGroup(externalProject.getGroup());
        if ("main".equals(sourceSet.getName())) {
          sourceSetData.setPublication(new ProjectId(externalProject.getGroup(),
                                                     externalProject.getName(),
                                                     externalProject.getVersion()));
        }
        sourceSetData.setVersion(externalProject.getVersion());
        sourceSetData.setIdeModuleGroup(moduleGroup);

        sourceSetData.internalSetSourceCompatibility(sourceSet.getSourceCompatibility());
        sourceSetData.internalSetTargetCompatibility(sourceSet.getTargetCompatibility());
        sourceSetData.internalSetSdkName(jdkName);

        final Set<File> artifacts = new THashSet<>(FileUtil.FILE_HASHING_STRATEGY);
        if ("main".equals(sourceSet.getName())) {
          final Set<File> defaultArtifacts = externalProject.getArtifactsByConfiguration().get("default");
          if (defaultArtifacts != null) {
            artifacts.addAll(defaultArtifacts);
          }
        }
        else {
          if ("test".equals(sourceSet.getName())) {
            sourceSetData.setProductionModuleId(getInternalModuleName(gradleModule, externalProject, "main", resolverCtx));
            final Set<File> testsArtifacts = externalProject.getArtifactsByConfiguration().get("tests");
            if (testsArtifacts != null) {
              artifacts.addAll(testsArtifacts);
            }
          }
        }
        artifacts.addAll(sourceSet.getArtifacts());
        for (ExternalSourceDirectorySet directorySet : sourceSet.getSources().values()) {
          artifacts.addAll(directorySet.getGradleOutputDirs());
        }
        sourceSetData.setArtifacts(new ArrayList<>(artifacts));

        DataNode<GradleSourceSetData> sourceSetDataNode = mainModuleNode.createChild(GradleSourceSetData.KEY, sourceSetData);
        final Map<String, Pair<DataNode<GradleSourceSetData>, ExternalSourceSet>> sourceSetMap =
          projectDataNode.getUserData(GradleProjectResolver.RESOLVED_SOURCE_SETS);
        assert sourceSetMap != null;
        sourceSetMap.put(moduleId, Pair.create(sourceSetDataNode, sourceSet));

        populateModuleSdkModel(gradleModule, sourceSetDataNode);
      }
    }
    else {
      try {
        IdeaJavaLanguageSettings languageSettings = gradleModule.getJavaLanguageSettings();
        if (languageSettings != null) {
          if (languageSettings.getLanguageLevel() != null) {
            mainModuleData.internalSetSourceCompatibility(languageSettings.getLanguageLevel().toString());
          }
          if (languageSettings.getTargetBytecodeVersion() != null) {
            mainModuleData.internalSetTargetCompatibility(languageSettings.getTargetBytecodeVersion().toString());
          }
        }
        mainModuleData.internalSetSdkName(jdkName);
      }
      catch (UnsupportedMethodException ignore) {
        // org.gradle.tooling.model.idea.IdeaModule.getJavaLanguageSettings method supported since Gradle 2.11
      }
    }

    populateModuleSdkModel(gradleModule, mainModuleNode);

    final ProjectData projectData = projectDataNode.getData();
    if (StringUtil.equals(mainModuleData.getLinkedExternalProjectPath(), projectData.getLinkedExternalProjectPath())) {
      projectData.setGroup(mainModuleData.getGroup());
      projectData.setVersion(mainModuleData.getVersion());
    }

    return mainModuleNode;
  }

  private static void populateProjectSdkModel(@NotNull IdeaProject ideaProject, @NotNull DataNode<? extends ProjectData> projectNode) {
    String sdkName = resolveJdkName(ideaProject.getJdkName());
    ProjectSdkData projectSdkData = new ProjectSdkData(sdkName);
    projectNode.createChild(ProjectSdkData.KEY, projectSdkData);
  }

  private static void populateModuleSdkModel(@NotNull IdeaModule ideaModule, @NotNull DataNode<? extends ModuleData> moduleNode) {
    String sdkName = resolveJdkName(ideaModule.getJdkName());
    ModuleSdkData moduleSdkData = new ModuleSdkData(sdkName);
    moduleNode.createChild(ModuleSdkData.KEY, moduleSdkData);
  }

  private static @Nullable String resolveJdkName(@Nullable String jdkNameOrVersion) {
    if (jdkNameOrVersion == null) return null;
    Sdk sdk = SdkLookupUtil.lookupSdk(builder -> builder
      .withSdkName(jdkNameOrVersion)
      .withSdkType(ExternalSystemJdkUtil.getJavaSdkType())
      .onDownloadableSdkSuggested(__ -> SdkLookupDecision.STOP)
    );
    return sdk == null ? null : sdk.getName();
  }

  private static String @NotNull [] getIdeModuleGroup(String moduleName, IdeaModule gradleModule) {
    final String gradlePath = gradleModule.getGradleProject().getPath();
    final String rootName = gradleModule.getProject().getName();
    if (isEmpty(gradlePath) || ":".equals(gradlePath)) {
      return new String[]{moduleName};
    }
    else {
      return (rootName + gradlePath).split(":");
    }
  }

  @Nullable
  private static String getJdkName(@NotNull IdeaModule gradleModule) {
    try {
      return gradleModule.getJdkName();
    }
    catch (UnsupportedMethodException e) {
      return null;
    }
  }

  @Override
  public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    GradleExtensions gradleExtensions = resolverCtx.getExtraProject(gradleModule, GradleExtensions.class);
    if (gradleExtensions != null) {
      boolean useCustomSerialization = Registry.is("gradle.tooling.custom.serializer", true);
      DefaultGradleExtensions extensions = useCustomSerialization ? (DefaultGradleExtensions)gradleExtensions
                                                                  : new DefaultGradleExtensions(gradleExtensions);
      ExternalProject externalProject = getExternalProject(gradleModule, resolverCtx);
      if (externalProject != null) {
        extensions.addTasks(externalProject.getTasks().values());
      }
      ideModule.createChild(GradleExtensionsDataService.KEY, extensions);
    }

    final IntelliJSettings intellijSettings = resolverCtx.getExtraProject(gradleModule, IntelliJSettings.class);
    if (intellijSettings != null) {
      ideModule.createChild(ProjectKeys.CONFIGURATION,
                            new ConfigurationDataImpl(GradleConstants.SYSTEM_ID, intellijSettings.getSettings()));
    }

    ProjectImportAction.AllModels models = resolverCtx.getModels();
    ExternalTestsModel externalTestsModel = models.getModel(gradleModule, ExternalTestsModel.class);
    if (externalTestsModel != null) {
      for (ExternalTestSourceMapping testSourceMapping : externalTestsModel.getTestSourceMappings()) {
        String testName = testSourceMapping.getTestName();
        String testTaskName = testSourceMapping.getTestTaskPath();
        Set<String> sourceFolders = testSourceMapping.getSourceFolders();
        TestData testData = new TestData(GradleConstants.SYSTEM_ID, testName, testTaskName, sourceFolders);
        ideModule.createChild(ProjectKeys.TEST, testData);
      }
    }
  }

  @Override
  public void populateModuleContentRoots(@NotNull IdeaModule gradleModule,
                                         @NotNull DataNode<ModuleData> ideModule) {
    ExternalProject externalProject = getExternalProject(gradleModule, resolverCtx);
    if (externalProject != null) {
      addExternalProjectContentRoots(gradleModule, ideModule, externalProject);
    }

    PathPrefixTreeMap<ContentRootData> contentRootIndex = new PathPrefixTreeMap<>();
    for (DataNode<ContentRootData> contentRootDataNode : ExternalSystemApiUtil.findAll(ideModule, ProjectKeys.CONTENT_ROOT)) {
      ContentRootData contentRootData = contentRootDataNode.getData();
      contentRootIndex.set(contentRootData.getRootPath(), contentRootData);
    }

    DomainObjectSet<? extends IdeaContentRoot> contentRoots = gradleModule.getContentRoots();
    if (contentRoots == null) return;
    for (IdeaContentRoot gradleContentRoot : contentRoots) {
      if (gradleContentRoot == null) continue;

      File rootDirectory = gradleContentRoot.getRootDirectory();
      if (rootDirectory == null) continue;

      boolean oldGradle = false;
      String contentRootPath = FileUtil.toCanonicalPath(rootDirectory.getAbsolutePath());
      ContentRootData ideContentRoot = new ContentRootData(GradleConstants.SYSTEM_ID, contentRootPath);
      contentRootIndex.set(contentRootPath, ideContentRoot);
      if (!resolverCtx.isResolveModulePerSourceSet()) {
        List<? extends IdeaSourceDirectory> sourceDirectories = gradleContentRoot.getSourceDirectories().getAll();
        List<? extends IdeaSourceDirectory> testDirectories = gradleContentRoot.getTestDirectories().getAll();
        List<? extends IdeaSourceDirectory> resourceDirectories = Collections.emptyList();
        List<? extends IdeaSourceDirectory> testResourceDirectories = Collections.emptyList();
        try {
          final Set<File> notResourceDirs = collectExplicitNonResourceDirectories(externalProject);

          resourceDirectories = gradleContentRoot.getResourceDirectories().getAll();
          removeDuplicateResources(sourceDirectories, resourceDirectories, notResourceDirs);
          testResourceDirectories = gradleContentRoot.getTestResourceDirectories().getAll();
          removeDuplicateResources(testDirectories, testResourceDirectories, notResourceDirs);
        }
        catch (UnsupportedMethodException e) {
          oldGradle = true;
          // org.gradle.tooling.model.idea.IdeaContentRoot.getResourceDirectories/getTestResourceDirectories methods supported since Gradle 4.7
          LOG.debug(e.getMessage());

          if (externalProject == null) {
            populateContentRoot(contentRootIndex, ExternalSystemSourceType.SOURCE, gradleContentRoot.getSourceDirectories());
            populateContentRoot(contentRootIndex, ExternalSystemSourceType.TEST, gradleContentRoot.getTestDirectories());
          }
        }

        if (!oldGradle) {
          populateContentRoot(contentRootIndex, ExternalSystemSourceType.SOURCE, sourceDirectories);
          populateContentRoot(contentRootIndex, ExternalSystemSourceType.TEST, testDirectories);
          populateContentRoot(contentRootIndex, ExternalSystemSourceType.RESOURCE, resourceDirectories);
          populateContentRoot(contentRootIndex, ExternalSystemSourceType.TEST_RESOURCE, testResourceDirectories);
        }
      }

      Set<File> excluded = gradleContentRoot.getExcludeDirectories();
      if (excluded != null) {
        for (File file : excluded) {
          ideContentRoot.storePath(ExternalSystemSourceType.EXCLUDED, file.getAbsolutePath());
        }
      }
    }
    Set<String> existsContentRoots = new LinkedHashSet<>();
    for (DataNode<ContentRootData> contentRootDataNode : ExternalSystemApiUtil.findAll(ideModule, ProjectKeys.CONTENT_ROOT)) {
      ContentRootData contentRootData = contentRootDataNode.getData();
      existsContentRoots.add(contentRootData.getRootPath());
    }
    for (ContentRootData ideContentRoot : contentRootIndex.getValues()) {
      if (!existsContentRoots.contains(ideContentRoot.getRootPath())) {
        ideModule.createChild(ProjectKeys.CONTENT_ROOT, ideContentRoot);
      }
    }
  }

  @Nullable
  private static ExternalProject getExternalProject(@NotNull IdeaModule gradleModule, @NotNull ProjectResolverContext resolverCtx) {
    ExternalProject project = resolverCtx.getExtraProject(gradleModule, ExternalProject.class);
    if (project == null && resolverCtx.isResolveModulePerSourceSet()) {
      LOG.error("External Project model is missing for module-per-sourceSet import mode. Please, check import log for error messages.");
    }
    return project;
  }

  private void addExternalProjectContentRoots(@NotNull IdeaModule gradleModule,
                                              @NotNull DataNode<ModuleData> ideModule,
                                              @NotNull ExternalProject externalProject) {
    processSourceSets(resolverCtx, gradleModule, externalProject, ideModule, new SourceSetsProcessor() {
      @Override
      public void process(@NotNull DataNode<? extends ModuleData> dataNode, @NotNull ExternalSourceSet sourceSet) {
        for (Map.Entry<? extends IExternalSystemSourceType, ? extends ExternalSourceDirectorySet> directorySetEntry : sourceSet.getSources().entrySet()) {
          ExternalSystemSourceType sourceType = ExternalSystemSourceType.from(directorySetEntry.getKey());
          ExternalSourceDirectorySet sourceDirectorySet = directorySetEntry.getValue();

          for (File file : sourceDirectorySet.getSrcDirs()) {
            ContentRootData ideContentRoot = new ContentRootData(GradleConstants.SYSTEM_ID, file.getAbsolutePath());
            ideContentRoot.storePath(sourceType, file.getAbsolutePath());
            dataNode.createChild(ProjectKeys.CONTENT_ROOT, ideContentRoot);
          }
        }
      }
    });
  }

  private static void removeDuplicateResources(@NotNull List<? extends IdeaSourceDirectory> sourceDirectories,
                                               @NotNull List<? extends IdeaSourceDirectory> resourceDirectories,
                                               @NotNull Set<File> notResourceDirs) {


    resourceDirectories.removeIf(ideaSourceDirectory -> notResourceDirs.contains(ideaSourceDirectory.getDirectory()));
    removeAll(sourceDirectories, resourceDirectories);
  }

  @NotNull
  private static Set<File> collectExplicitNonResourceDirectories(@Nullable ExternalProject externalProject) {
    if (externalProject == null) {
      return Collections.emptySet();
    }

    return externalProject.getSourceSets().values().stream()
      .flatMap(ss -> ss.getSources().entrySet().stream()
        .filter(e -> !e.getKey().isResource())
        .flatMap(e -> e.getValue().getSrcDirs().stream()))
      .collect(Collectors.toCollection(() -> new THashSet<>(FileUtil.FILE_HASHING_STRATEGY)));
  }

  private static void removeAll(List<? extends IdeaSourceDirectory> list, List<? extends IdeaSourceDirectory> toRemove) {
    Set<File> files = toRemove.stream().map(o -> o.getDirectory()).collect(Collectors.toSet());
    list.removeIf(o -> files.contains(o.getDirectory()));
  }

  private static void processSourceSets(@NotNull ProjectResolverContext resolverCtx,
                                        @NotNull IdeaModule gradleModule,
                                        @NotNull ExternalProject externalProject,
                                        @NotNull DataNode<ModuleData> ideModule,
                                        @NotNull SourceSetsProcessor processor) {
    Map<String, DataNode<GradleSourceSetData>> sourceSetsMap = new HashMap<>();
    for (DataNode<GradleSourceSetData> dataNode : ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY)) {
      sourceSetsMap.put(dataNode.getData().getId(), dataNode);
    }

    for (ExternalSourceSet sourceSet : externalProject.getSourceSets().values()) {
      if (sourceSet == null || sourceSet.getSources().isEmpty()) continue;

      final String moduleId = getModuleId(resolverCtx, gradleModule, sourceSet);
      final DataNode<? extends ModuleData> moduleDataNode = sourceSetsMap.isEmpty() ? ideModule : sourceSetsMap.get(moduleId);
      if (moduleDataNode == null) continue;

      processor.process(moduleDataNode, sourceSet);
    }
  }


  @Override
  public void populateModuleCompileOutputSettings(@NotNull IdeaModule gradleModule,
                                                  @NotNull DataNode<ModuleData> ideModule) {
    ModuleData moduleData = ideModule.getData();
    moduleData.useExternalCompilerOutput(resolverCtx.isDelegatedBuild());

    File ideaOutDir = new File(moduleData.getLinkedExternalProjectPath(), "out");

    ExternalProject externalProject = getExternalProject(gradleModule, resolverCtx);
    if (resolverCtx.isResolveModulePerSourceSet()) {
      DataNode<ProjectData> projectDataNode = ideModule.getDataNode(ProjectKeys.PROJECT);
      assert projectDataNode != null;
      final Map<String, Pair<String, ExternalSystemSourceType>> moduleOutputsMap = projectDataNode.getUserData(MODULES_OUTPUTS);
      assert moduleOutputsMap != null;

      Set<String> outputDirs = new HashSet<>();
      assert externalProject != null;
      processSourceSets(resolverCtx, gradleModule, externalProject, ideModule, new SourceSetsProcessor() {
        @Override
        public void process(@NotNull DataNode<? extends ModuleData> dataNode, @NotNull ExternalSourceSet sourceSet) {
          MultiMap<ExternalSystemSourceType, String> gradleOutputMap = dataNode.getUserData(GradleProjectResolver.GRADLE_OUTPUTS);
          if (gradleOutputMap == null) {
            gradleOutputMap = MultiMap.create();
            dataNode.putUserData(GradleProjectResolver.GRADLE_OUTPUTS, gradleOutputMap);
          }
          final ModuleData moduleData = dataNode.getData();
          moduleData.useExternalCompilerOutput(resolverCtx.isDelegatedBuild());
          for (Map.Entry<? extends IExternalSystemSourceType, ? extends ExternalSourceDirectorySet> directorySetEntry : sourceSet.getSources().entrySet()) {
            ExternalSystemSourceType sourceType = ExternalSystemSourceType.from(directorySetEntry.getKey());
            ExternalSourceDirectorySet sourceDirectorySet = directorySetEntry.getValue();
            File ideOutputDir = getIdeOutputDir(sourceDirectorySet);
            File gradleOutputDir = getGradleOutputDir(sourceDirectorySet);
            File outputDir = resolverCtx.isDelegatedBuild() ? gradleOutputDir : ideOutputDir;
            moduleData.setCompileOutputPath(sourceType, ideOutputDir == null ? null : ideOutputDir.getAbsolutePath());
            moduleData.setExternalCompilerOutputPath(sourceType, gradleOutputDir == null ? null : gradleOutputDir.getAbsolutePath());
            moduleData.setInheritProjectCompileOutputPath(sourceDirectorySet.isCompilerOutputPathInherited());

            if (outputDir != null) {
              outputDirs.add(outputDir.getPath());
              for (File file : sourceDirectorySet.getGradleOutputDirs()) {
                String gradleOutputPath = ExternalSystemApiUtil.toCanonicalPath(file.getAbsolutePath());
                gradleOutputMap.putValue(sourceType, gradleOutputPath);
                if (!file.getPath().equals(outputDir.getPath())) {
                  moduleOutputsMap.put(gradleOutputPath, Pair.create(moduleData.getId(), sourceType));
                }
              }
            }
          }
        }
      });
      if (outputDirs.stream().anyMatch(path -> FileUtil.isAncestor(ideaOutDir, new File(path), false))) {
        excludeOutDir(ideModule, ideaOutDir);
      }
      return;
    }

    IdeaCompilerOutput moduleCompilerOutput = gradleModule.getCompilerOutput();
    boolean inheritOutputDirs = moduleCompilerOutput != null && moduleCompilerOutput.getInheritOutputDirs();

    if (moduleCompilerOutput != null) {
      File outputDir = moduleCompilerOutput.getOutputDir();
      if (outputDir != null) {
        moduleData.setCompileOutputPath(ExternalSystemSourceType.SOURCE, outputDir.getAbsolutePath());
        moduleData.setCompileOutputPath(ExternalSystemSourceType.RESOURCE, outputDir.getAbsolutePath());
        moduleData.setExternalCompilerOutputPath(ExternalSystemSourceType.SOURCE, outputDir.getAbsolutePath());
        moduleData.setExternalCompilerOutputPath(ExternalSystemSourceType.RESOURCE, outputDir.getAbsolutePath());
      }
      else {
        moduleData.setCompileOutputPath(ExternalSystemSourceType.SOURCE, new File(ideaOutDir, "production/classes").getAbsolutePath());
        moduleData.setCompileOutputPath(ExternalSystemSourceType.RESOURCE, new File(ideaOutDir, "production/resources").getAbsolutePath());
        if (externalProject != null) {
          File gradleOutputDir = getGradleOutputDir(externalProject, "main", ExternalSystemSourceType.SOURCE);
          moduleData.setExternalCompilerOutputPath(ExternalSystemSourceType.SOURCE,
                                                   gradleOutputDir == null ? null : gradleOutputDir.getAbsolutePath());
          File gradleResourceOutputDir = getGradleOutputDir(externalProject, "main", ExternalSystemSourceType.RESOURCE);
          moduleData.setExternalCompilerOutputPath(ExternalSystemSourceType.RESOURCE,
                                                   gradleResourceOutputDir == null ? null : gradleResourceOutputDir.getAbsolutePath());
        }
      }

      File testOutputDir = moduleCompilerOutput.getTestOutputDir();
      if (testOutputDir != null) {
        moduleData.setCompileOutputPath(ExternalSystemSourceType.TEST, testOutputDir.getAbsolutePath());
        moduleData.setCompileOutputPath(ExternalSystemSourceType.TEST_RESOURCE, testOutputDir.getAbsolutePath());
        moduleData.setExternalCompilerOutputPath(ExternalSystemSourceType.TEST, testOutputDir.getAbsolutePath());
        moduleData.setExternalCompilerOutputPath(ExternalSystemSourceType.TEST_RESOURCE, testOutputDir.getAbsolutePath());
      }
      else {
        moduleData.setCompileOutputPath(ExternalSystemSourceType.TEST, new File(ideaOutDir, "test/classes").getAbsolutePath());
        moduleData.setCompileOutputPath(ExternalSystemSourceType.TEST_RESOURCE, new File(ideaOutDir, "test/resources").getAbsolutePath());
        if (externalProject != null) {
          File gradleOutputDir = getGradleOutputDir(externalProject, "test", ExternalSystemSourceType.TEST);
          moduleData.setExternalCompilerOutputPath(ExternalSystemSourceType.TEST,
                                                   gradleOutputDir == null ? null : gradleOutputDir.getAbsolutePath());
          File gradleResourceOutputDir = getGradleOutputDir(externalProject, "test", ExternalSystemSourceType.TEST_RESOURCE);
          moduleData.setExternalCompilerOutputPath(ExternalSystemSourceType.TEST_RESOURCE,
                                                   gradleResourceOutputDir == null ? null : gradleResourceOutputDir.getAbsolutePath());
        }
      }

      if (!resolverCtx.isDelegatedBuild() && !inheritOutputDirs && (outputDir == null || testOutputDir == null)) {
        excludeOutDir(ideModule, ideaOutDir);
      }
    }

    moduleData.setInheritProjectCompileOutputPath(inheritOutputDirs);
  }

  @Nullable
  public static File getGradleOutputDir(@NotNull ExternalProject externalProject,
                                        @NotNull String sourceSetName,
                                        @NotNull ExternalSystemSourceType sourceType) {
    ExternalSourceSet sourceSet = externalProject.getSourceSets().get(sourceSetName);
    if (sourceSet == null) return null;
    return getGradleOutputDir(sourceSet.getSources().get(sourceType));
  }

  @Nullable
  private static File getIdeOutputDir(@Nullable ExternalSourceDirectorySet sourceDirectorySet) {
    if (sourceDirectorySet == null) return null;
    return sourceDirectorySet.getOutputDir();
  }

  @Nullable
  private static File getGradleOutputDir(@Nullable ExternalSourceDirectorySet sourceDirectorySet) {
    if (sourceDirectorySet == null) return null;
    return sourceDirectorySet.getGradleOutputDirs().stream().findFirst().orElse(null);
  }

  private static void excludeOutDir(@NotNull DataNode<ModuleData> ideModule, File ideaOutDir) {
    ContentRootData excludedContentRootData;
    DataNode<ContentRootData> contentRootDataDataNode = ExternalSystemApiUtil.find(ideModule, ProjectKeys.CONTENT_ROOT);
    if (contentRootDataDataNode == null ||
        !FileUtil.isAncestor(new File(contentRootDataDataNode.getData().getRootPath()), ideaOutDir, false)) {
      excludedContentRootData = new ContentRootData(GradleConstants.SYSTEM_ID, ideaOutDir.getAbsolutePath());
      ideModule.createChild(ProjectKeys.CONTENT_ROOT, excludedContentRootData);
    }
    else {
      excludedContentRootData = contentRootDataDataNode.getData();
    }

    excludedContentRootData.storePath(ExternalSystemSourceType.EXCLUDED, ideaOutDir.getAbsolutePath());
  }

  @Override
  public void populateModuleDependencies(@NotNull IdeaModule gradleModule,
                                         @NotNull DataNode<ModuleData> ideModule,
                                         @NotNull final DataNode<ProjectData> ideProject) {

    ExternalProject externalProject = getExternalProject(gradleModule, resolverCtx);
    if (resolverCtx.isResolveModulePerSourceSet()) {
      final Map<String, Pair<DataNode<GradleSourceSetData>, ExternalSourceSet>> sourceSetMap =
        ideProject.getUserData(GradleProjectResolver.RESOLVED_SOURCE_SETS);
      final Map<String, String> artifactsMap = ideProject.getUserData(CONFIGURATION_ARTIFACTS);
      assert sourceSetMap != null;
      assert artifactsMap != null;
      assert externalProject != null;
      processSourceSets(resolverCtx, gradleModule, externalProject, ideModule, new SourceSetsProcessor() {
        @Override
        public void process(@NotNull DataNode<? extends ModuleData> dataNode, @NotNull ExternalSourceSet sourceSet) {
          buildDependencies(resolverCtx, sourceSetMap, artifactsMap, dataNode, sourceSet.getDependencies(), ideProject);
        }
      });
      return;
    }

    final List<? extends IdeaDependency> dependencies = gradleModule.getDependencies().getAll();

    if (dependencies == null) return;

    List<String> orphanModules = new ArrayList<>();
    Map<String, ModuleData> modulesIndex = new HashMap<>();

    for (DataNode<ModuleData> dataNode : ExternalSystemApiUtil.getChildren(ideProject, ProjectKeys.MODULE)) {
      modulesIndex.put(dataNode.getData().getExternalName(), dataNode.getData());
    }

    for (int i = 0; i < dependencies.size(); i++) {
      IdeaDependency dependency = dependencies.get(i);
      if (dependency == null) {
        continue;
      }
      DependencyScope scope = parseScope(dependency.getScope());

      if (dependency instanceof IdeaModuleDependency) {
        ModuleDependencyData d = buildDependency(resolverCtx, ideModule, (IdeaModuleDependency)dependency, modulesIndex);
        d.setExported(dependency.getExported());
        if (scope != null) {
          d.setScope(scope);
        }
        d.setOrder(i);
        ideModule.createChild(ProjectKeys.MODULE_DEPENDENCY, d);
        ModuleData targetModule = d.getTarget();
        if (targetModule.getId().isEmpty() && targetModule.getLinkedExternalProjectPath().isEmpty()) {
          orphanModules.add(targetModule.getExternalName());
        }
      }
      else if (dependency instanceof IdeaSingleEntryLibraryDependency) {
        LibraryDependencyData d = buildDependency(gradleModule, ideModule, (IdeaSingleEntryLibraryDependency)dependency, ideProject);
        d.setExported(dependency.getExported());
        if (scope != null) {
          d.setScope(scope);
        }
        d.setOrder(i);
        ideModule.createChild(ProjectKeys.LIBRARY_DEPENDENCY, d);
      }
    }

    if (!orphanModules.isEmpty()) {
      ExternalSystemTaskId taskId = resolverCtx.getExternalSystemTaskId();
      Project project = taskId.findProject();
      if (project != null) {
        String msg =
          "Can't find the following module" + (orphanModules.size() > 1 ? "s" : "") + ": " + join(orphanModules, ", ")
          + "\nIt can be caused by composite build configuration inside your *.gradle scripts with Gradle version older than 3.3." +
          "\nTry Gradle 3.3 or better or enable 'Create separate module per source set' option";
        NotificationData notification = new NotificationData(
          "Gradle project structure problems", msg, NotificationCategory.WARNING, NotificationSource.PROJECT_SYNC);
        ExternalSystemNotificationManager.getInstance(project).showNotification(taskId.getProjectSystemId(), notification);
      }
    }
  }

  @NotNull
  @Override
  public Collection<TaskData> populateModuleTasks(@NotNull IdeaModule gradleModule,
                                                  @NotNull DataNode<ModuleData> ideModule,
                                                  @NotNull DataNode<ProjectData> ideProject)
    throws IllegalArgumentException, IllegalStateException {

    final Collection<TaskData> tasks = new ArrayList<>();
    final String moduleConfigPath = ideModule.getData().getLinkedExternalProjectPath();

    String rootProjectPath = ideProject.getData().getLinkedExternalProjectPath();
    try {
      File rootDir = gradleModule.getGradleProject().getProjectIdentifier().getBuildIdentifier().getRootDir();
      rootProjectPath = ExternalSystemApiUtil.toCanonicalPath(rootDir.getCanonicalPath());
    }
    catch (IOException e) {
      LOG.warn("construction of the canonical path for the module fails", e);
    }

    ExternalProject externalProject = getExternalProject(gradleModule, resolverCtx);
    if (externalProject != null) {
      final boolean isFlatProject = !FileUtil.isAncestor(rootProjectPath, moduleConfigPath, false);
      for (ExternalTask task : externalProject.getTasks().values()) {
        String taskName = isFlatProject ? task.getQName() : task.getName();
        String taskGroup = task.getGroup();
        if (taskName.trim().isEmpty() || isIdeaTask(taskName, taskGroup)) {
          continue;
        }
        final String taskPath = isFlatProject ? rootProjectPath : moduleConfigPath;
        TaskData taskData = new TaskData(GradleConstants.SYSTEM_ID, taskName, taskPath, task.getDescription());
        taskData.setGroup(taskGroup);
        taskData.setType(task.getType());
        taskData.setTest(task.isTest());
        ideModule.createChild(ProjectKeys.TASK, taskData);
        taskData.setInherited(StringUtil.equals(task.getName(), task.getQName()));
        tasks.add(taskData);
      }

      return tasks;
    }

    for (GradleTask task : gradleModule.getGradleProject().getTasks()) {
      String taskName = task.getName();
      String taskGroup = getTaskGroup(task);
      if (taskName == null || taskName.trim().isEmpty() || isIdeaTask(taskName, taskGroup)) {
        continue;
      }
      TaskData taskData = new TaskData(GradleConstants.SYSTEM_ID, taskName, moduleConfigPath, task.getDescription());
      taskData.setGroup(taskGroup);
      ideModule.createChild(ProjectKeys.TASK, taskData);
      tasks.add(taskData);
    }

    return tasks;
  }

  @Nullable
  private static String getTaskGroup(GradleTask task) {
    String taskGroup;
    try {
      taskGroup = task.getGroup();
    }
    catch (UnsupportedMethodException e) {
      taskGroup = null;
    }
    return taskGroup;
  }

  @NotNull
  @Override
  public Set<Class<?>> getExtraProjectModelClasses() {
    return newLinkedHashSet(
      BuildScriptClasspathModel.class,
      GradleExtensions.class,
      ExternalTestsModel.class,
      IntelliJProjectSettings.class,
      IntelliJSettings.class
    );
  }

  @NotNull
  @Override
  public ProjectImportModelProvider getModelProvider() {
    return new ClassSetImportModelProvider(getExtraProjectModelClasses(), newLinkedHashSet(ExternalProject.class, IdeaProject.class));
  }

  @Override
  public Set<Class<?>> getTargetTypes() {
    return newLinkedHashSet(
      ExternalProjectDependency.class,
      ExternalLibraryDependency.class,
      FileCollectionDependency.class,
      UnresolvedExternalDependency.class
    );
  }

  @Override
  public void enhanceTaskProcessing(@NotNull List<String> taskNames,
                                    @NotNull Consumer<String> initScriptConsumer,
                                    @NotNull Map<String, String> parameters) {
    String dispatchPort = parameters.get(GradleProjectResolverExtension.DEBUG_DISPATCH_PORT_KEY);
    if (dispatchPort == null) {
      return;
    }

    String debugOptions = parameters.get(GradleProjectResolverExtension.DEBUG_OPTIONS_KEY);
    if (debugOptions == null) {
      debugOptions = "";
    }
    List<String> lines = new ArrayList<>();

    String esRtJarPath = FileUtil.toCanonicalPath(PathManager.getJarPathForClass(ExternalSystemSourceType.class));
    lines.add("initscript { dependencies { classpath files(\""+ esRtJarPath + "\") } }"); // bring external-system-rt.jar

    for (DebuggerBackendExtension extension: DebuggerBackendExtension.EP_NAME.getExtensionList()) {
      lines.addAll(extension.initializationCode(dispatchPort, debugOptions));
    }

    final String script = join(lines, SystemProperties.getLineSeparator());
    initScriptConsumer.consume(script);
  }

  /**
   * Stores information about given directories at the corresponding to content root
   *
   * @param contentRootIndex index of content roots
   * @param type             type of data located at the given directories
   * @param dirs             directories which paths should be stored at the given content root
   * @throws IllegalArgumentException if specified by {@link ContentRootData#storePath(ExternalSystemSourceType, String)}
   */
  private static void populateContentRoot(@NotNull final PathPrefixTreeMap<ContentRootData> contentRootIndex,
                                          @NotNull final ExternalSystemSourceType type,
                                          @Nullable final Iterable<? extends IdeaSourceDirectory> dirs)
    throws IllegalArgumentException {
    if (dirs == null) {
      return;
    }
    for (IdeaSourceDirectory dir : dirs) {
      ExternalSystemSourceType dirSourceType = type;
      try {
        if (dir.isGenerated() && !dirSourceType.isGenerated()) {
          final ExternalSystemSourceType generatedType = ExternalSystemSourceType.from(
            dirSourceType.isTest(), dir.isGenerated(), dirSourceType.isResource(), dirSourceType.isExcluded()
          );
          dirSourceType = generatedType != null ? generatedType : dirSourceType;
        }
      }
      catch (UnsupportedMethodException e) {
        // org.gradle.tooling.model.idea.IdeaSourceDirectory.isGenerated method supported only since Gradle 2.2
        LOG.warn(e.getMessage());
        printToolingProxyDiagnosticInfo(dir);
      }
      catch (Throwable e) {
        LOG.debug(e);
        printToolingProxyDiagnosticInfo(dir);
      }
      String path = FileUtil.toCanonicalPath(dir.getDirectory().getAbsolutePath());
      if (contentRootIndex.getAllAncestorKeys(path).isEmpty()) {
        ContentRootData contentRootData = new ContentRootData(GradleConstants.SYSTEM_ID, path);
        contentRootIndex.set(path, contentRootData);
      }
      List<String> ancestors = contentRootIndex.getAllAncestorKeys(path);
      String contentRootPath = ancestors.get(ancestors.size() - 1);
      ContentRootData contentRoot = contentRootIndex.get(contentRootPath);
      assert contentRoot != null;
      contentRoot.storePath(dirSourceType, path);
    }
  }

  private static void printToolingProxyDiagnosticInfo(@Nullable Object obj) {
    if (!LOG.isDebugEnabled() || obj == null) return;

    LOG.debug(String.format("obj: %s", obj));
    final Class<?> aClass = obj.getClass();
    LOG.debug(String.format("obj class: %s", aClass));
    LOG.debug(String.format("classloader: %s", aClass.getClassLoader()));
    for (Method m : aClass.getDeclaredMethods()) {
      LOG.debug(String.format("obj m: %s", m));
    }

    if (obj instanceof Proxy) {
      try {
        final Field hField = ReflectionUtil.findField(obj.getClass(), null, "h");
        hField.setAccessible(true);
        final Object h = hField.get(obj);
        final Field delegateField = ReflectionUtil.findField(h.getClass(), null, "delegate");
        delegateField.setAccessible(true);
        final Object delegate = delegateField.get(h);
        LOG.debug(String.format("delegate: %s", delegate));
        LOG.debug(String.format("delegate class: %s", delegate.getClass()));
        LOG.debug(String.format("delegate classloader: %s", delegate.getClass().getClassLoader()));
        for (Method m : delegate.getClass().getDeclaredMethods()) {
          LOG.debug(String.format("delegate m: %s", m));
        }
      }
      catch (NoSuchFieldException | IllegalAccessException e) {
        LOG.debug(e);
      }
    }
  }

  @Nullable
  private static DependencyScope parseScope(@Nullable IdeaDependencyScope scope) {
    if (scope == null) {
      return null;
    }
    String scopeAsString = scope.getScope();
    if (scopeAsString == null) {
      return null;
    }
    for (DependencyScope dependencyScope : DependencyScope.values()) {
      if (scopeAsString.equalsIgnoreCase(dependencyScope.toString())) {
        return dependencyScope;
      }
    }
    return null;
  }

  @NotNull
  private static ModuleDependencyData buildDependency(@NotNull ProjectResolverContext resolverContext,
                                                      @NotNull DataNode<ModuleData> ownerModule,
                                                      @NotNull IdeaModuleDependency dependency,
                                                      @NotNull Map<String, ModuleData> registeredModulesIndex)
    throws IllegalStateException {

    final GradleExecutionSettings gradleExecutionSettings = resolverContext.getSettings();
    final String projectGradleVersionString = resolverContext.getProjectGradleVersion();
    if (gradleExecutionSettings != null && projectGradleVersionString != null) {
      final GradleVersion projectGradleVersion = GradleVersion.version(projectGradleVersionString);
      if (projectGradleVersion.compareTo(GradleVersion.version("4.0")) < 0) {
        final IdeaModule dependencyModule = getDependencyModuleByReflection(dependency);
        if (dependencyModule != null) {
          final ModuleData moduleData =
            gradleExecutionSettings.getExecutionWorkspace().findModuleDataByModule(resolverContext, dependencyModule);
          if (moduleData != null) {
            return new ModuleDependencyData(ownerModule.getData(), moduleData);
          }
        }
      }
    }


    final String moduleName = dependency.getTargetModuleName();

    if (gradleExecutionSettings != null) {
      ModuleData moduleData = gradleExecutionSettings.getExecutionWorkspace().findModuleDataByGradleModuleName(moduleName);
      if (moduleData != null) {
        return new ModuleDependencyData(ownerModule.getData(), moduleData);
      }
    }

    ModuleData registeredModuleData = registeredModulesIndex.get(moduleName);
    if (registeredModuleData != null) {
      return new ModuleDependencyData(ownerModule.getData(), registeredModuleData);
    }

    throw new IllegalStateException(String.format(
      "Can't parse gradle module dependency '%s'. Reason: no module with such name (%s) is found. Registered modules: %s",
      dependency, moduleName, registeredModulesIndex.keySet()
    ));
  }

  @Nullable
  private static IdeaModule getDependencyModuleByReflection(@NotNull IdeaModuleDependency dependency) {
    Method getDependencyModule = ReflectionUtil.getMethod(dependency.getClass(), "getDependencyModule");
    if (getDependencyModule != null) {
      try {
        Object result = getDependencyModule.invoke(dependency);
        return (IdeaModule)result;
      }
      catch (IllegalAccessException e) {
        LOG.info("Failed to get dependency module for [" + dependency + "]", e);
      }
      catch (InvocationTargetException e) {
        LOG.info("Failed to get dependency module for [" + dependency + "]", e);
      }
    }
    return null;
  }

  @NotNull
  private LibraryDependencyData buildDependency(@NotNull IdeaModule gradleModule,
                                                @NotNull DataNode<ModuleData> ownerModule,
                                                @NotNull IdeaSingleEntryLibraryDependency dependency,
                                                @NotNull DataNode<ProjectData> ideProject)
    throws IllegalStateException {
    File binaryPath = dependency.getFile();
    if (binaryPath == null) {
      throw new IllegalStateException(String.format(
        "Can't parse external library dependency '%s'. Reason: it doesn't specify path to the binaries", dependency
      ));
    }

    String libraryName;
    LibraryLevel level;
    final GradleModuleVersion moduleVersion = dependency.getGradleModuleVersion();

    // Gradle API doesn't explicitly provide information about unresolved libraries
    // original discussion http://issues.gradle.org/browse/GRADLE-1995
    // github issue https://github.com/gradle/gradle/issues/7733
    // That's why we use this dirty hack here.
    boolean unresolved = binaryPath.getName().startsWith(UNRESOLVED_DEPENDENCY_PREFIX);

    if (moduleVersion == null) {
      if (binaryPath.isFile()) {
        boolean isModuleLocalLibrary = false;
        try {
          isModuleLocalLibrary = FileUtil.isAncestor(gradleModule.getGradleProject().getProjectDirectory(), binaryPath, false);
        }
        catch (UnsupportedMethodException e) {
          // ignore, generate project-level library for the dependency
        }
        if (isModuleLocalLibrary) {
          level = LibraryLevel.MODULE;
        }
        else {
          level = LibraryLevel.PROJECT;
        }

        libraryName = chooseName(binaryPath, level, ideProject);
      }
      else {
        level = LibraryLevel.MODULE;
        libraryName = "";
      }

      if (unresolved) {
        // Gradle uses names like 'unresolved dependency - commons-collections commons-collections 3.2' for unresolved dependencies.
        libraryName = binaryPath.getName().substring(UNRESOLVED_DEPENDENCY_PREFIX.length());
        libraryName = join(split(libraryName, " "), ":");
      }
    }
    else {
      level = LibraryLevel.PROJECT;
      libraryName = String.format("%s:%s:%s", moduleVersion.getGroup(), moduleVersion.getName(), moduleVersion.getVersion());
      if (binaryPath.isFile()) {
        String libraryFileName = FileUtilRt.getNameWithoutExtension(binaryPath.getName());
        final String mavenLibraryFileName = String.format("%s-%s", moduleVersion.getName(), moduleVersion.getVersion());
        if (!mavenLibraryFileName.equals(libraryFileName)) {
          Pattern pattern = Pattern.compile(moduleVersion.getName() + "-" + moduleVersion.getVersion() + "-(.*)");
          Matcher matcher = pattern.matcher(libraryFileName);
          if (matcher.matches()) {
            final String classifier = matcher.group(1);
            libraryName += (":" + classifier);
          }
          else {
            final String artifactId = trimEnd(trimEnd(libraryFileName, moduleVersion.getVersion()), "-");
            libraryName = String.format("%s:%s:%s",
                                        moduleVersion.getGroup(),
                                        artifactId,
                                        moduleVersion.getVersion());
          }
        }
      }
    }

    // add packaging type to distinguish different artifact dependencies with same groupId:artifactId:version
    if (!unresolved && isNotEmpty(libraryName) && !FileUtilRt.extensionEquals(binaryPath.getName(), "jar")) {
      libraryName += (":" + FileUtilRt.getExtension(binaryPath.getName()));
    }

    LibraryData library = new LibraryData(GradleConstants.SYSTEM_ID, libraryName, unresolved);
    if (moduleVersion != null) {
      library.setGroup(moduleVersion.getGroup());
      library.setArtifactId(moduleVersion.getName());
      library.setVersion(moduleVersion.getVersion());
    }

    if (!unresolved) {
      library.addPath(LibraryPathType.BINARY, binaryPath.getAbsolutePath());
    }
    else {
      boolean isOfflineWork = resolverCtx.getSettings() != null && resolverCtx.getSettings().isOfflineWork();
      String message = String.format("Could not resolve %s.", libraryName);
      BuildIssue buildIssue = new UnresolvedDependencySyncIssue(libraryName, message, resolverCtx.getProjectPath(), isOfflineWork);
      resolverCtx.report(MessageEvent.Kind.ERROR, buildIssue);
    }

    File sourcePath = dependency.getSource();
    if (!unresolved && sourcePath != null) {
      library.addPath(LibraryPathType.SOURCE, sourcePath.getAbsolutePath());
    }

    if (!unresolved && sourcePath == null) {
      attachGradleSdkSources(gradleModule, binaryPath, library, resolverCtx);
      if (resolverCtx instanceof DefaultProjectResolverContext) {
        attachSourcesAndJavadocFromGradleCacheIfNeeded(resolverCtx,
                                                       ((DefaultProjectResolverContext)resolverCtx).getGradleUserHome(), library);
      }
    }

    File javadocPath = dependency.getJavadoc();
    if (!unresolved && javadocPath != null) {
      library.addPath(LibraryPathType.DOC, javadocPath.getAbsolutePath());
    }

    if (level == LibraryLevel.PROJECT && !linkProjectLibrary(resolverCtx, ideProject, library)) {
      level = LibraryLevel.MODULE;
    }

    return new LibraryDependencyData(ownerModule.getData(), library, level);
  }

  private String chooseName(File path,
                            LibraryLevel level,
                            DataNode<ProjectData> ideProject) {
    final String fileName = FileUtilRt.getNameWithoutExtension(path.getName());
    if (level == LibraryLevel.MODULE) {
      return fileName;
    }
    else {
      int count = 0;
      while (true) {
        String candidateName = fileName + (count == 0 ? "" : "_" + count);
        DataNode<LibraryData> libraryData =
          ExternalSystemApiUtil.find(ideProject, ProjectKeys.LIBRARY,
                                     node -> node.getData().getExternalName().equals(candidateName));
        if (libraryData != null) {
          if (libraryData.getData().getPaths(LibraryPathType.BINARY).contains(FileUtil.toSystemIndependentName(path.getAbsolutePath()))) {
            return candidateName;
          }
          else {
            count++;
          }
        }
        else {
          return candidateName;
        }
      }
    }
  }

  private interface SourceSetsProcessor {
    void process(@NotNull DataNode<? extends ModuleData> dataNode, @NotNull ExternalSourceSet sourceSet);
  }
}
