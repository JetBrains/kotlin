// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.execution.CommandLineUtil;
import com.intellij.externalSystem.JavaModuleData;
import com.intellij.externalSystem.JavaProjectData;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependencies;
import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.gradle.api.JavaVersion;
import org.gradle.tooling.model.idea.IdeaJavaLanguageSettings;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.*;
import org.jetbrains.plugins.gradle.model.data.AnnotationProcessingData;
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.UNORDERED)
public class JavaGradleProjectResolver extends AbstractProjectResolverExtension {
  private final static Logger LOG = Logger.getInstance(JavaGradleProjectResolver.class);

  @Override
  public void populateProjectExtraModels(@NotNull IdeaProject gradleProject, @NotNull DataNode<ProjectData> ideProject) {
    String compileOutputPath = getCompileOutputPath();
    IdeaJavaLanguageSettings languageSettings = gradleProject.getJavaLanguageSettings();
    LanguageLevel languageLevel = getLanguageLevel(languageSettings, false);
    String targetBytecodeVersion = getTargetBytecodeVersion(languageSettings);

    JavaSdkVersion jdkVersion = JavaProjectData.resolveSdkVersion(gradleProject.getJdkName());

    JavaProjectData javaProjectData =
      new JavaProjectData(GradleConstants.SYSTEM_ID, compileOutputPath, jdkVersion, languageLevel, targetBytecodeVersion);

    ideProject.createChild(JavaProjectData.KEY, javaProjectData);

    nextResolver.populateProjectExtraModels(gradleProject, ideProject);
  }

  @NotNull
  private String getCompileOutputPath() {
    String projectDirPath = resolverCtx.getProjectPath();
    // Gradle API doesn't expose gradleProject compile output path yet.
    return projectDirPath + "/build/classes";
  }

  @Override
  public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    populateBuildScriptClasspathData(gradleModule, ideModule);
    populateAnnotationProcessorData(gradleModule, ideModule);
    populateDependenciesGraphData(gradleModule, ideModule);
    nextResolver.populateModuleExtraModels(gradleModule, ideModule);
  }

  private void populateAnnotationProcessorData(@NotNull IdeaModule gradleModule,
                                               @NotNull DataNode<ModuleData> ideModule) {
    final AnnotationProcessingModel apModel = resolverCtx.getExtraProject(gradleModule, AnnotationProcessingModel.class);
    if (apModel == null) {
      return;
    }
    if (!resolverCtx.isResolveModulePerSourceSet()) {
      final AnnotationProcessingData apData = getMergedAnnotationProcessingData(apModel);
      DataNode<AnnotationProcessingData> dataNode = ideModule.createChild(AnnotationProcessingData.KEY, apData);
      populateAnnotationProcessingOutput(dataNode, apModel);
    } else {
      Collection<DataNode<GradleSourceSetData>> all = ExternalSystemApiUtil.findAll(ideModule, GradleSourceSetData.KEY);
      for (DataNode<GradleSourceSetData> node : all) {
        final AnnotationProcessingData apData = getAnnotationProcessingData(apModel, node.getData().getModuleName());
        if (apData != null) {
          DataNode<AnnotationProcessingData> dataNode = node.createChild(AnnotationProcessingData.KEY, apData);
          populateAnnotationProcessorOutput(dataNode, apModel, node.getData().getModuleName());
        }
      }
    }
  }

  private static void populateAnnotationProcessorOutput(@NotNull DataNode<AnnotationProcessingData> parent,
                                                        @NotNull AnnotationProcessingModel apModel,
                                                        @NotNull String sourceSetName) {
    AnnotationProcessingConfig config = apModel.bySourceSetName(sourceSetName);
    if (config != null && config.getProcessorOutput() != null) {
      parent.createChild(AnnotationProcessingData.OUTPUT_KEY,
                         new AnnotationProcessingData.AnnotationProcessorOutput(config.getProcessorOutput(), config.isTestSources()));
    }
  }

  private static void populateAnnotationProcessingOutput(@NotNull DataNode<AnnotationProcessingData> parent,
                                                         @NotNull AnnotationProcessingModel apModel) {
    for (AnnotationProcessingConfig config : apModel.allConfigs().values()) {
      if (config.getProcessorOutput() != null) {
        parent.createChild(AnnotationProcessingData.OUTPUT_KEY,
                           new AnnotationProcessingData.AnnotationProcessorOutput(config.getProcessorOutput(), config.isTestSources()));
      }
    }
  }

  @NotNull
  private static AnnotationProcessingData getMergedAnnotationProcessingData(@NotNull AnnotationProcessingModel apModel) {

    final Set<String> mergedAnnotationProcessorPath = new LinkedHashSet<>();
    for (AnnotationProcessingConfig config : apModel.allConfigs().values()) {
      mergedAnnotationProcessorPath.addAll(config.getAnnotationProcessorPath());
    }

    final List<String> apArguments = new ArrayList<>();
    final AnnotationProcessingConfig mainConfig = apModel.bySourceSetName("main");
    if (mainConfig != null) {
       apArguments.addAll(mainConfig.getAnnotationProcessorArguments());
    }

    return AnnotationProcessingData.create(mergedAnnotationProcessorPath, apArguments);
  }

  @Nullable
  private static AnnotationProcessingData getAnnotationProcessingData(@NotNull AnnotationProcessingModel apModel,
                                                                      @NotNull String sourceSetName) {
    AnnotationProcessingConfig config = apModel.bySourceSetName(sourceSetName);
    if (config == null) {
      return null;
    } else {
      return AnnotationProcessingData.create(config.getAnnotationProcessorPath(),
                                             config.getAnnotationProcessorArguments());
    }
  }

  private void populateBuildScriptClasspathData(@NotNull IdeaModule gradleModule,
                                                @NotNull DataNode<ModuleData> ideModule) {
    final BuildScriptClasspathModel buildScriptClasspathModel = resolverCtx.getExtraProject(gradleModule, BuildScriptClasspathModel.class);
    final List<BuildScriptClasspathData.ClasspathEntry> classpathEntries;
    if (buildScriptClasspathModel != null) {
      classpathEntries = ContainerUtil.map(
        buildScriptClasspathModel.getClasspath(),
        (Function<ClasspathEntryModel, BuildScriptClasspathData.ClasspathEntry>)model -> BuildScriptClasspathData.ClasspathEntry
          .create(model.getClasses(), model.getSources(), model.getJavadoc()));
    }
    else {
      classpathEntries = ContainerUtil.emptyList();
    }
    BuildScriptClasspathData buildScriptClasspathData = new BuildScriptClasspathData(GradleConstants.SYSTEM_ID, classpathEntries);
    buildScriptClasspathData.setGradleHomeDir(buildScriptClasspathModel != null ? buildScriptClasspathModel.getGradleHomeDir() : null);
    ideModule.createChild(BuildScriptClasspathData.KEY, buildScriptClasspathData);
  }

  private void populateDependenciesGraphData(@NotNull IdeaModule gradleModule,
                                             @NotNull DataNode<ModuleData> ideModule) {
    final ProjectDependencies projectDependencies = resolverCtx.getExtraProject(gradleModule, ProjectDependencies.class);
    if (projectDependencies != null) {
      ideModule.createChild(ProjectKeys.DEPENDENCIES_GRAPH, projectDependencies);
    }
  }

  @Override
  public void enhanceTaskProcessing(@NotNull List<String> taskNames,
                                    @NotNull Consumer<String> initScriptConsumer,
                                    @NotNull Map<String, String> parameters) {
    String testExecutionExpected = parameters.get(GradleProjectResolverExtension.TEST_EXECUTION_EXPECTED_KEY);

    if (Boolean.valueOf(testExecutionExpected)) {
      try (InputStream stream = getClass().getResourceAsStream("/org/jetbrains/plugins/gradle/java/addTestListener.groovy")) {
        String addTestListenerScript = StreamUtil.readText(stream, StandardCharsets.UTF_8);
        initScriptConsumer.consume(addTestListenerScript);
      }
      catch (IOException e) {
        LOG.info(e);
      }
    }

    String jvmParametersSetup = parameters.get(GradleProjectResolverExtension.JVM_PARAMETERS_SETUP_KEY);
    enhanceTaskProcessing(taskNames, jvmParametersSetup, initScriptConsumer);
  }

  private String loadTestEventListenerDefinition() {
    try(InputStream stream = getClass().getResourceAsStream("/org/jetbrains/plugins/gradle/IJTestLogger.groovy")) {
      return StreamUtil.readText(stream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      LOG.info(e);
    }
    return "";
  }

  @Override
  public void enhanceTaskProcessing(@NotNull List<String> taskNames,
                                    @Nullable String jvmParametersSetup,
                                    @NotNull Consumer<String> initScriptConsumer) {
    if (!StringUtil.isEmpty(jvmParametersSetup)) {
      LOG.assertTrue(!jvmParametersSetup.contains(ForkedDebuggerHelper.JVM_DEBUG_SETUP_PREFIX),
                     "Please use org.jetbrains.plugins.gradle.service.debugger.GradleJvmDebuggerBackend to setup debugger");

      final String names = "[" + toStringListLiteral(taskNames, ", ") + "]";
      List<String> argv = ParametersListUtil.parse(jvmParametersSetup);
      if (SystemInfo.isWindows) {
        argv = ContainerUtil.map(argv, s -> CommandLineUtil.escapeParameterOnWindows(s, false));
      }
      final String jvmArgs = toStringListLiteral(argv, " << ");

      final String[] lines = {
        "gradle.taskGraph.beforeTask { Task task ->",
        "    if (task instanceof JavaForkOptions && (" + names + ".contains(task.name) || " + names + ".contains(task.path))) {",
        "        def jvmArgs = task.jvmArgs.findAll{!it?.startsWith('-agentlib:jdwp') && !it?.startsWith('-Xrunjdwp')}",
        "        jvmArgs << " + jvmArgs,
        "        task.jvmArgs = jvmArgs",
        "    }",
        "}",
      };
      final String script = StringUtil.join(lines, SystemProperties.getLineSeparator());
      initScriptConsumer.consume(script);
    }

    final String testEventListenerDefinition = loadTestEventListenerDefinition();
    initScriptConsumer.consume(testEventListenerDefinition);
  }

  @NotNull
  private static String toStringListLiteral(@NotNull List<String> strings, @NotNull String separator) {
    final List<String> quotedStrings = ContainerUtil.map(strings, s -> toStringLiteral(s));
    return StringUtil.join(quotedStrings, separator);
  }

  @NotNull
  private static String toStringLiteral(@NotNull String s) {
    return StringUtil.wrapWithDoubleQuote(StringUtil.escapeStringCharacters(s));
  }

  @NotNull
  @Override
  public Set<Class<?>> getExtraProjectModelClasses() {
    return ContainerUtil.set(AnnotationProcessingModel.class, ProjectDependencies.class);
  }

  @Override
  public @Nullable DataNode<ModuleData> createModule(@NotNull IdeaModule gradleModule, @NotNull DataNode<ProjectData> projectDataNode) {
    DataNode<ModuleData> moduleNode = super.createModule(gradleModule, projectDataNode);
    if (moduleNode == null) return null;
    ExternalProject externalProject = resolverCtx.getExtraProject(gradleModule, ExternalProject.class);
    if (externalProject == null) return moduleNode;
    if (resolverCtx.isResolveModulePerSourceSet()) {
      Map<ExternalSourceSet, DataNode<GradleSourceSetData>> sourceSets = findSourceSets(gradleModule, externalProject, moduleNode);
      for (Map.Entry<ExternalSourceSet, DataNode<GradleSourceSetData>> entry : sourceSets.entrySet()) {
        JavaModuleData moduleData = createSourceSetModuleData(gradleModule, entry.getKey());
        entry.getValue().createChild(JavaModuleData.KEY, moduleData);
      }
    }
    JavaModuleData moduleData = createMainModuleData(gradleModule, externalProject);
    moduleNode.createChild(JavaModuleData.KEY, moduleData);
    return moduleNode;
  }

  private static @NotNull JavaModuleData createMainModuleData(@NotNull IdeaModule ideaModule, @NotNull ExternalProject externalProject) {
    IdeaJavaLanguageSettings javaLanguageSettings = ideaModule.getJavaLanguageSettings();
    LanguageLevel languageLevel = getLanguageLevel(externalProject, false);
    if (languageLevel == null) languageLevel = getLanguageLevel(javaLanguageSettings, false);
    String targetBytecodeVersion = externalProject.getTargetCompatibility();
    if (targetBytecodeVersion == null) targetBytecodeVersion = getTargetBytecodeVersion(javaLanguageSettings);
    return new JavaModuleData(GradleConstants.SYSTEM_ID, languageLevel, targetBytecodeVersion);
  }

  private static @NotNull JavaModuleData createSourceSetModuleData(@NotNull IdeaModule ideaModule, @NotNull ExternalSourceSet sourceSet) {
    IdeaJavaLanguageSettings javaLanguageSettings = ideaModule.getJavaLanguageSettings();
    LanguageLevel languageLevel = getLanguageLevel(sourceSet);
    if (languageLevel == null) languageLevel = getLanguageLevel(javaLanguageSettings, sourceSet.isPreview());
    String targetBytecodeVersion = sourceSet.getTargetCompatibility();
    if (targetBytecodeVersion == null) targetBytecodeVersion = getTargetBytecodeVersion(javaLanguageSettings);
    return new JavaModuleData(GradleConstants.SYSTEM_ID, languageLevel, targetBytecodeVersion);
  }

  private @NotNull Map<ExternalSourceSet, DataNode<GradleSourceSetData>> findSourceSets(
    @NotNull IdeaModule ideaModule,
    @NotNull ExternalProject externalProject,
    @NotNull DataNode<ModuleData> moduleNode
  ) {
    Collection<DataNode<GradleSourceSetData>> sourceSetNodes = ExternalSystemApiUtil.getChildren(moduleNode, GradleSourceSetData.KEY);
    Map<String, DataNode<GradleSourceSetData>> sourceSetIndex = new LinkedHashMap<>();
    for (DataNode<GradleSourceSetData> sourceSetNode : sourceSetNodes) {
      sourceSetIndex.put(sourceSetNode.getData().getId(), sourceSetNode);
    }
    Map<ExternalSourceSet, DataNode<GradleSourceSetData>> result = new LinkedHashMap<>();
    for (ExternalSourceSet sourceSet : externalProject.getSourceSets().values()) {
      String moduleId = GradleProjectResolverUtil.getModuleId(resolverCtx, ideaModule, sourceSet);
      DataNode<GradleSourceSetData> sourceSetNode = sourceSetIndex.get(moduleId);
      if (sourceSetNode == null) continue;
      result.put(sourceSet, sourceSetNode);
    }
    return result;
  }

  private static @Nullable LanguageLevel getLanguageLevel(@NotNull ExternalSourceSet sourceSet) {
    String sourceCompatibility = sourceSet.getSourceCompatibility();
    if (sourceCompatibility == null) return null;
    return parseLanguageLevel(sourceCompatibility, sourceSet.isPreview());
  }

  @SuppressWarnings("SameParameterValue")
  private static @Nullable LanguageLevel getLanguageLevel(@NotNull ExternalProject externalProject, boolean isPreview) {
    String sourceCompatibility = externalProject.getSourceCompatibility();
    if (sourceCompatibility == null) return null;
    return parseLanguageLevel(sourceCompatibility, isPreview);
  }

  private static @Nullable LanguageLevel getLanguageLevel(@Nullable IdeaJavaLanguageSettings languageSettings, boolean isPreview) {
    if (languageSettings == null) return null;
    JavaVersion languageLevel = languageSettings.getLanguageLevel();
    if (languageLevel == null) return null;
    return parseLanguageLevel(languageLevel.toString(), isPreview);
  }

  private static @Nullable LanguageLevel parseLanguageLevel(@NotNull String languageLevelString, boolean isPreview) {
    LanguageLevel languageLevel = LanguageLevel.parse(languageLevelString);
    if (languageLevel == null) return null;
    return setPreview(languageLevel, isPreview);
  }

  private static @NotNull LanguageLevel setPreview(@NotNull LanguageLevel languageLevel, boolean isPreview) {
    if (languageLevel.isPreview() == isPreview) return languageLevel;
    com.intellij.util.lang.JavaVersion javaVersion = languageLevel.toJavaVersion();
    return Arrays.stream(LanguageLevel.values())
      .filter(it -> it.isPreview() == isPreview)
      .filter(it -> it.toJavaVersion().equals(javaVersion))
      .findFirst()
      .orElse(languageLevel);
  }

  private static @Nullable String getTargetBytecodeVersion(@Nullable IdeaJavaLanguageSettings languageSettings) {
    if (languageSettings == null) return null;
    JavaVersion targetByteCodeVersion = languageSettings.getTargetBytecodeVersion();
    if (targetByteCodeVersion == null) return null;
    return targetByteCodeVersion.toString();
  }
}
