// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.externalSystem.JavaProjectData;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerConfiguration;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.AnnotationProcessingConfig;
import org.jetbrains.plugins.gradle.model.AnnotationProcessingModel;
import org.jetbrains.plugins.gradle.model.BuildScriptClasspathModel;
import org.jetbrains.plugins.gradle.model.ClasspathEntryModel;
import org.jetbrains.plugins.gradle.model.data.AnnotationProcessingData;
import org.jetbrains.plugins.gradle.model.data.BuildScriptClasspathData;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.UNORDERED)
public class JavaGradleProjectResolver extends AbstractProjectResolverExtension {
  private final static Logger LOG = Logger.getInstance(JavaGradleProjectResolver.class);
  @Override
  public void populateProjectExtraModels(@NotNull IdeaProject gradleProject, @NotNull DataNode<ProjectData> ideProject) {
    // import java project data
    final String projectDirPath = resolverCtx.getProjectPath();

    // Gradle API doesn't expose gradleProject compile output path yet.
    JavaProjectData javaProjectData = new JavaProjectData(GradleConstants.SYSTEM_ID, projectDirPath + "/build/classes");

    IdeaProject ideaProject = resolverCtx.getModels().getModel(IdeaProject.class);
    if (ideaProject != null) {
      javaProjectData.setJdkVersion(ideaProject.getJdkName());
    }
    LanguageLevel resolvedLanguageLevel = null;
    // org.gradle.tooling.model.idea.IdeaLanguageLevel.getLevel() returns something like JDK_1_6
    String languageLevel = null;
    if (ideaProject != null) {
      languageLevel = ideaProject.getLanguageLevel().getLevel();
      for (LanguageLevel level : LanguageLevel.values()) {
        if (level.name().equals(languageLevel)) {
          resolvedLanguageLevel = level;
          break;
        }
      }
    }
    if (resolvedLanguageLevel != null) {
      javaProjectData.setLanguageLevel(resolvedLanguageLevel);
    }
    else {
      javaProjectData.setLanguageLevel(languageLevel);
    }

    ideProject.createChild(JavaProjectData.KEY, javaProjectData);

    nextResolver.populateProjectExtraModels(gradleProject, ideProject);
  }

  @Override
  public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    populateBuildScriptClasspathData(gradleModule, ideModule);
    populateAnnotationProcessorData(gradleModule, ideModule);
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
        (Function<ClasspathEntryModel, BuildScriptClasspathData.ClasspathEntry>)model -> BuildScriptClasspathData.ClasspathEntry.create(model.getClasses(), model.getSources(), model.getJavadoc()));
    }
    else {
      classpathEntries = ContainerUtil.emptyList();
    }
    BuildScriptClasspathData buildScriptClasspathData = new BuildScriptClasspathData(GradleConstants.SYSTEM_ID, classpathEntries);
    buildScriptClasspathData.setGradleHomeDir(buildScriptClasspathModel != null ? buildScriptClasspathModel.getGradleHomeDir() : null);
    ideModule.createChild(BuildScriptClasspathData.KEY, buildScriptClasspathData);
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
      ForkedDebuggerConfiguration forkedDebuggerSetup = ForkedDebuggerConfiguration.parse(jvmParametersSetup);
      if (forkedDebuggerSetup == null) {
        final String names = "[\"" + StringUtil.join(taskNames, "\", \"") + "\"]";
        final String jvmArgs = Arrays.stream(ParametersListUtil.parseToArray(jvmParametersSetup))
          .map(s -> '\'' + s.trim().replace("\\", "\\\\") + '\'').collect(Collectors.joining(" << "));
        final String[] lines = {
          "gradle.taskGraph.beforeTask { Task task ->",
          "    if (task instanceof JavaForkOptions && (" + names + ".contains(task.name) || " + names + ".contains(task.path))) {",
          "        def jvmArgs = task.jvmArgs.findAll{!it?.startsWith('-agentlib:jdwp') && !it?.startsWith('-Xrunjdwp')}",
          "        jvmArgs << " + jvmArgs,
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

  @NotNull
  @Override
  public Set<Class> getExtraProjectModelClasses() {
    return Collections.singleton(AnnotationProcessingModel.class);
  }
}
