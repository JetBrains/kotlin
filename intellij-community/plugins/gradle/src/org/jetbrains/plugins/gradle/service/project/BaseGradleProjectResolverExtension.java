// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.google.gson.GsonBuilder;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ConfigurationDataImpl;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerConfiguration;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager;
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.externalSystem.service.notification.NotificationSource;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.externalSystem.util.PathPrefixTreeMap;
import com.intellij.openapi.externalSystem.util.PathPrefixTreeMapImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import com.intellij.util.PathUtil;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.text.CharArrayUtil;
import org.codehaus.groovy.runtime.typehandling.ShortTypeHandling;
import org.gradle.internal.impldep.com.google.common.collect.Multimap;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.UnsupportedMethodException;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.gradle.tooling.model.idea.*;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.*;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;
import org.jetbrains.plugins.gradle.model.tests.ExternalTestSourceMapping;
import org.jetbrains.plugins.gradle.model.tests.ExternalTestsModel;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataService;
import org.jetbrains.plugins.gradle.service.project.data.GradleExtensionsDataService;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;
import org.jetbrains.plugins.gradle.tooling.builder.ModelBuildScriptClasspathBuilderImpl;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.intellij.openapi.util.Pair.pair;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolver.CONFIGURATION_ARTIFACTS;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolver.MODULES_OUTPUTS;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.*;

/**
 * {@link BaseGradleProjectResolverExtension} provides base implementation of Gradle project resolver.
 *
 * @author Vladislav.Soroka
 */
@Order(Integer.MAX_VALUE)
public class BaseGradleProjectResolverExtension implements GradleProjectResolverExtension {
  private static final Logger LOG = Logger.getInstance(BaseGradleProjectResolverExtension.class);

  @NotNull @NonNls private static final String UNRESOLVED_DEPENDENCY_PREFIX = "unresolved dependency - ";

  @NotNull private ProjectResolverContext resolverCtx;
  @NotNull private final BaseProjectImportErrorHandler myErrorHandler = new BaseProjectImportErrorHandler();

  @Override
  public void setProjectResolverContext(@NotNull ProjectResolverContext projectResolverContext) {
    resolverCtx = projectResolverContext;
  }

  @Override
  public void setNext(@NotNull GradleProjectResolverExtension next) {
    // should be the last extension in the chain
  }

  @Nullable
  @Override
  public GradleProjectResolverExtension getNext() {
    return null;
  }

  @NotNull
  @Override
  public ProjectData createProject() {
    final String projectDirPath = resolverCtx.getProjectPath();
    final ExternalProject externalProject = resolverCtx.getExtraProject(ExternalProject.class);
    String projectName = externalProject != null ? externalProject.getName() : resolverCtx.getModels().getIdeaProject().getName();
    return new ProjectData(GradleConstants.SYSTEM_ID, projectName, projectDirPath, projectDirPath);
  }

  @Override
  public void populateProjectExtraModels(@NotNull IdeaProject gradleProject, @NotNull DataNode<ProjectData> ideProject) {
    final ExternalProject externalProject = resolverCtx.getExtraProject(ExternalProject.class);
    if (externalProject != null) {
      ideProject.createChild(ExternalProjectDataService.KEY, externalProject);
      ideProject.getData().setDescription(externalProject.getDescription());
    }

    final IntelliJSettings intellijSettings = resolverCtx.getExtraProject(IntelliJProjectSettings.class);
    if (intellijSettings != null) {
      ideProject.createChild(ProjectKeys.CONFIGURATION,
                             new ConfigurationDataImpl(GradleConstants.SYSTEM_ID, intellijSettings.getSettings()));
    }
  }

  @NotNull
  @Override
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

    ExternalProject externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject.class);
    if (resolverCtx.isResolveModulePerSourceSet() && externalProject != null) {
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

        sourceSetData.setSourceCompatibility(sourceSet.getSourceCompatibility());
        sourceSetData.setTargetCompatibility(sourceSet.getTargetCompatibility());
        sourceSetData.setSdkName(jdkName);

        final Set<File> artifacts = ContainerUtil.newTroveSet(FileUtil.FILE_HASHING_STRATEGY);
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
      }
    }
    else {
      try {
        IdeaJavaLanguageSettings languageSettings = gradleModule.getJavaLanguageSettings();
        if (languageSettings != null) {
          if (languageSettings.getLanguageLevel() != null) {
            mainModuleData.setSourceCompatibility(languageSettings.getLanguageLevel().toString());
          }
          if (languageSettings.getTargetBytecodeVersion() != null) {
            mainModuleData.setTargetCompatibility(languageSettings.getTargetBytecodeVersion().toString());
          }
        }
        mainModuleData.setSdkName(jdkName);
      }
      catch (UnsupportedMethodException ignore) {
        // org.gradle.tooling.model.idea.IdeaModule.getJavaLanguageSettings method supported since Gradle 2.11
      }
    }

    final ProjectData projectData = projectDataNode.getData();
    if (StringUtil.equals(mainModuleData.getLinkedExternalProjectPath(), projectData.getLinkedExternalProjectPath())) {
      projectData.setGroup(mainModuleData.getGroup());
      projectData.setVersion(mainModuleData.getVersion());
    }

    return mainModuleNode;
  }

  @NotNull
  protected String[] getIdeModuleGroup(String moduleName, IdeaModule gradleModule) {
    String[] moduleGroup;
    final String gradlePath = gradleModule.getGradleProject().getPath();
    final String rootName = gradleModule.getProject().getName();
    final boolean isRootModule = StringUtil.isEmpty(gradlePath) || ":".equals(gradlePath);
    moduleGroup = isRootModule
                  ? new String[]{ moduleName }
                  : (rootName + gradlePath).split(":");
    return moduleGroup;
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
      DefaultGradleExtensions extensions = new DefaultGradleExtensions(gradleExtensions);
      ExternalProject externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject.class);
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
    ExternalTestsModel externalTestsModel = models.getExtraProject(gradleModule, ExternalTestsModel.class);
    if (externalTestsModel != null) {
      for (ExternalTestSourceMapping testSourceMapping : externalTestsModel.getTestSourceMappings()) {
        String testName = testSourceMapping.getTestName();
        String testTaskName = testSourceMapping.getTestTaskPath();
        String cleanTestTaskName = testSourceMapping.getCleanTestTaskPath();
        Set<String> sourceFolders = testSourceMapping.getSourceFolders();
        TestData testData = new TestData(GradleConstants.SYSTEM_ID, testName, testTaskName, cleanTestTaskName, sourceFolders);
        ideModule.createChild(ProjectKeys.TEST, testData);
      }
    }
  }

  @Override
  public void populateModuleContentRoots(@NotNull IdeaModule gradleModule,
                                         @NotNull DataNode<ModuleData> ideModule) {
    ExternalProject externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject.class);
    if (externalProject != null) {
      addExternalProjectContentRoots(gradleModule, ideModule, externalProject);
    } else if (resolverCtx.isResolveModulePerSourceSet()) {
      LOG.error("External Project model is missing for module-per-sourceSet import mode. Please, check import log for error messages.");
    }

    PathPrefixTreeMap<ContentRootData> contentRootIndex = new PathPrefixTreeMapImpl<>();
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
      .collect(Collectors.toCollection(() -> ContainerUtil.newTroveSet(FileUtil.FILE_HASHING_STRATEGY)));
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

    ExternalProject externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject.class);
    if (resolverCtx.isResolveModulePerSourceSet() && externalProject != null) {
      DataNode<ProjectData> projectDataNode = ideModule.getDataNode(ProjectKeys.PROJECT);
      assert projectDataNode != null;
      final Map<String, Pair<String, ExternalSystemSourceType>> moduleOutputsMap = projectDataNode.getUserData(MODULES_OUTPUTS);
      assert moduleOutputsMap != null;

      Set<String> outputDirs = new HashSet<>();
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
  private static File getGradleOutputDir(@NotNull ExternalProject externalProject,
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

    ExternalProject externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject.class);
    if (externalProject != null) {
      final Map<String, Pair<DataNode<GradleSourceSetData>, ExternalSourceSet>> sourceSetMap =
        ideProject.getUserData(GradleProjectResolver.RESOLVED_SOURCE_SETS);

      final Map<String, String> artifactsMap = ideProject.getUserData(CONFIGURATION_ARTIFACTS);
      assert artifactsMap != null;

      if (resolverCtx.isResolveModulePerSourceSet()) {
        assert sourceSetMap != null;
        processSourceSets(resolverCtx, gradleModule, externalProject, ideModule, new SourceSetsProcessor() {
          @Override
          public void process(@NotNull DataNode<? extends ModuleData> dataNode, @NotNull ExternalSourceSet sourceSet) {
            buildDependencies(resolverCtx, sourceSetMap, artifactsMap, dataNode, sourceSet.getDependencies(), ideProject);
          }
        });

        return;
      }
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
          "Can't find the following module" + (orphanModules.size() > 1 ? "s" : "") + ": " + StringUtil.join(orphanModules, ", ")
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

    ExternalProject externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject.class);
    String rootProjectPath = ideProject.getData().getLinkedExternalProjectPath();
    try {
      GradleBuild build = resolverCtx.getExtraProject(gradleModule, GradleBuild.class);
      if (build != null) {
        rootProjectPath = ExternalSystemApiUtil.toCanonicalPath(build.getRootProject().getProjectDirectory().getCanonicalPath());
      }
    }
    catch (IOException e) {
      LOG.warn("construction of the canonical path for the module fails", e);
    }

    final boolean isFlatProject = !FileUtil.isAncestor(rootProjectPath, moduleConfigPath, false);
    if (externalProject != null) {
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
  public Set<Class> getExtraProjectModelClasses() {
    Set<Class> result = ContainerUtil.set(GradleBuild.class, ModuleExtendedModel.class);
    result.add(BuildScriptClasspathModel.class);
    result.add(GradleExtensions.class);
    result.add(ExternalProject.class);
    result.add(ExternalTestsModel.class);
    result.add(IntelliJProjectSettings.class);
    result.add(IntelliJSettings.class);
    return result;
  }

  @NotNull
  @Override
  public ProjectImportExtraModelProvider getExtraModelProvider() {
    return new ClassSetProjectImportExtraModelProvider(getExtraProjectModelClasses());
  }

  @NotNull
  @Override
  public Set<Class> getToolingExtensionsClasses() {
    return ContainerUtil.set(
      // external-system-rt.jar
      ExternalSystemSourceType.class,
      // gradle-tooling-extension-api jar
      ProjectImportAction.class,
      // gradle-tooling-extension-impl jar
      ModelBuildScriptClasspathBuilderImpl.class,
      // repacked gradle guava
      Multimap.class,
      GsonBuilder.class,
      ShortTypeHandling.class
    );
  }

  @Override
  public Set<Class> getTargetTypes() {
    return ContainerUtil.set(
      ExternalProjectDependency.class,
      ExternalLibraryDependency.class,
      FileCollectionDependency.class,
      UnresolvedExternalDependency.class
    );
  }

  @NotNull
  @Override
  public List<Pair<String, String>> getExtraJvmArgs() {
    if (ExternalSystemApiUtil.isInProcessMode(GradleConstants.SYSTEM_ID)) {
      final List<Pair<String, String>> extraJvmArgs = new ArrayList<>();

      final HttpConfigurable httpConfigurable = HttpConfigurable.getInstance();
      if (!StringUtil.isEmpty(httpConfigurable.PROXY_EXCEPTIONS)) {
        List<String> hosts = StringUtil.split(httpConfigurable.PROXY_EXCEPTIONS, ",");
        if (!hosts.isEmpty()) {
          final String nonProxyHosts = StringUtil.join(hosts, StringUtil.TRIMMER, "|");
          extraJvmArgs.add(pair("http.nonProxyHosts", nonProxyHosts));
          extraJvmArgs.add(pair("https.nonProxyHosts", nonProxyHosts));
        }
      }
      if (httpConfigurable.USE_HTTP_PROXY && StringUtil.isNotEmpty(httpConfigurable.getProxyLogin())) {
        extraJvmArgs.add(pair("http.proxyUser", httpConfigurable.getProxyLogin()));
        extraJvmArgs.add(pair("https.proxyUser", httpConfigurable.getProxyLogin()));
        final String plainProxyPassword = httpConfigurable.getPlainProxyPassword();
        extraJvmArgs.add(pair("http.proxyPassword", plainProxyPassword));
        extraJvmArgs.add(pair("https.proxyPassword", plainProxyPassword));
      }
      extraJvmArgs.addAll(httpConfigurable.getJvmProperties(false, null));

      return extraJvmArgs;
    }

    return Collections.emptyList();
  }

  @NotNull
  @Override
  public List<String> getExtraCommandLineArgs() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public ExternalSystemException getUserFriendlyError(@Nullable BuildEnvironment buildEnvironment,
                                                      @NotNull Throwable error,
                                                      @NotNull String projectPath,
                                                      @Nullable String buildFilePath) {
    return myErrorHandler.getUserFriendlyError(buildEnvironment, error, projectPath, buildFilePath);
  }

  @Override
  public void preImportCheck() {
  }

  @Override
  public void enhanceTaskProcessing(@NotNull List<String> taskNames,
                                    @Nullable String jvmAgentSetup,
                                    @NotNull Consumer<String> initScriptConsumer) {
    if (!StringUtil.isEmpty(jvmAgentSetup)) {
      ForkedDebuggerConfiguration forkedDebuggerSetup = ForkedDebuggerConfiguration.parse(jvmAgentSetup);
      if (forkedDebuggerSetup != null) {
        setupDebugForAllJvmForkedTasks(initScriptConsumer, forkedDebuggerSetup.getForkSocketPort());
      }
      else {
        final String names = "[\"" + StringUtil.join(taskNames, "\", \"") + "\"]";
        final String[] lines = {
          "gradle.taskGraph.beforeTask { Task task ->",
          "    if (task instanceof JavaForkOptions && (" + names + ".contains(task.name) || " + names + ".contains(task.path))) {",
          "        def jvmArgs = task.jvmArgs.findAll{!it?.startsWith('-agentlib:jdwp') && !it?.startsWith('-Xrunjdwp')}",
          "        jvmArgs << '" + jvmAgentSetup.trim().replace("\\", "\\\\") + '\'',
          "        task.jvmArgs = jvmArgs",
          "    }" +
          "}",
        };
        final String script = StringUtil.join(lines, SystemProperties.getLineSeparator());
        initScriptConsumer.consume(script);
      }
    }

    final String testEventListenerDefinition = loadTestEventListenerDefinition();
    initScriptConsumer.consume(testEventListenerDefinition);
  }

  private String loadTestEventListenerDefinition() {
    try(InputStream stream = getClass().getResourceAsStream("/org/jetbrains/plugins/gradle/IJTestLogger.groovy")) {
      return StreamUtil.readText(stream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOG.info(e);
    }
    return "";
  }

  public void setupDebugForAllJvmForkedTasks(@NotNull Consumer<? super String> initScriptConsumer, int debugPort) {
    // external-system-rt.jar
    String esRtJarPath = PathUtil.getCanonicalPath(PathManager.getJarPathForClass(ExternalSystemSourceType.class));
    final String[] lines = {
      "initscript {",
      "  dependencies {",
      "    classpath files(\"" + esRtJarPath + "\")",
      "  }",
      "}",
      "gradle.taskGraph.beforeTask { Task task ->",
      " if (task instanceof org.gradle.api.tasks.testing.Test) {",
      "  task.maxParallelForks = 1",
      "  task.forkEvery = 0",
      " }",
      " if (task instanceof JavaForkOptions) {",
      "  def jvmArgs = task.jvmArgs.findAll{!it?.startsWith('-agentlib:jdwp') && !it?.startsWith('-Xrunjdwp')}",
      "  jvmArgs << com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper.setupDebugger(task.path, " + debugPort + ")",
      "  task.jvmArgs = jvmArgs",
      " }",
      "}",
      "gradle.taskGraph.afterTask { Task task ->",
      "    if (task instanceof JavaForkOptions) {",
      "        com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper.processFinished(task.path, " + debugPort + ")",
      "    }",
      "}",
    };
    final String script = StringUtil.join(lines, SystemProperties.getLineSeparator());
    initScriptConsumer.consume(script);
  }

  @Override
  public void enhanceRemoteProcessing(@NotNull SimpleJavaParameters parameters) {
    // IntelliJ Gradle integration uses in-process calls for gradle tooling api
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
        int i = libraryName.indexOf(' ');
        if (i >= 0) {
          i = CharArrayUtil.shiftForward(libraryName, i + 1, " ");
        }

        if (i >= 0 && i < libraryName.length()) {
          int dependencyNameIndex = i;
          i = libraryName.indexOf(' ', dependencyNameIndex);
          if (i > 0) {
            libraryName = String.format("%s-%s", libraryName.substring(dependencyNameIndex, i), libraryName.substring(i + 1));
          }
        }
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
            final String artifactId = StringUtil.trimEnd(StringUtil.trimEnd(libraryFileName, moduleVersion.getVersion()), "-");
            libraryName = String.format("%s:%s:%s",
                                        moduleVersion.getGroup(),
                                        artifactId,
                                        moduleVersion.getVersion());
          }
        }
      }
    }

    // add packaging type to distinguish different artifact dependencies with same groupId:artifactId:version
    if (StringUtil.isNotEmpty(libraryName) && !FileUtilRt.extensionEquals(binaryPath.getName(), "jar")) {
      libraryName += (":" + FileUtilRt.getExtension(binaryPath.getName()));
    }

    final LibraryData library = new LibraryData(GradleConstants.SYSTEM_ID, libraryName, unresolved);
    if (moduleVersion != null) {
      library.setGroup(moduleVersion.getGroup());
      library.setArtifactId(moduleVersion.getName());
      library.setVersion(moduleVersion.getVersion());
    }

    if (!unresolved) {
      library.addPath(LibraryPathType.BINARY, binaryPath.getAbsolutePath());
    }

    File sourcePath = dependency.getSource();
    if (!unresolved && sourcePath != null) {
      library.addPath(LibraryPathType.SOURCE, sourcePath.getAbsolutePath());
    }

    if (!unresolved && sourcePath == null) {
      attachGradleSdkSources(gradleModule, binaryPath, library, resolverCtx);
      if (resolverCtx instanceof DefaultProjectResolverContext) {
        attachSourcesAndJavadocFromGradleCacheIfNeeded(((DefaultProjectResolverContext)resolverCtx).getGradleUserHome(), library);
      }
    }

    File javadocPath = dependency.getJavadoc();
    if (!unresolved && javadocPath != null) {
      library.addPath(LibraryPathType.DOC, javadocPath.getAbsolutePath());
    }

    if (level == LibraryLevel.PROJECT && !linkProjectLibrary(ideProject, library)) {
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
