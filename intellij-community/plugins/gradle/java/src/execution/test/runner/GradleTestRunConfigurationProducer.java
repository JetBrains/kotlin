// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution.test.runner;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.TestData;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.plugins.gradle.execution.GradleRunnerUtil;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.TestRunner;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.GradleUtil;
import org.jetbrains.plugins.gradle.util.TasksToRun;

import java.util.*;

import static com.intellij.openapi.externalSystem.util.ExternalSystemUtil.getExternalProjectInfo;
import static com.intellij.openapi.util.text.StringUtil.endsWithChar;
import static org.jetbrains.plugins.gradle.execution.test.runner.TestGradleConfigurationProducerUtilKt.escapeIfNeeded;
import static org.jetbrains.plugins.gradle.settings.TestRunner.*;

/**
 * @author Vladislav.Soroka
 */
public abstract class GradleTestRunConfigurationProducer extends RunConfigurationProducer<ExternalSystemRunConfiguration> {
  private static final List<String> TEST_SOURCE_SET_TASKS = Arrays.asList("cleanTest", "test");

  protected static final Logger LOG = Logger.getInstance(GradleTestRunConfigurationProducer.class);

  private TestTasksChooser testTasksChooser = new TestTasksChooser();

  /**
   * @deprecated Override {@link #getConfigurationFactory()}.
   */
  @Deprecated
  protected GradleTestRunConfigurationProducer(ConfigurationType configurationType) {
    super(configurationType);
  }

  protected GradleTestRunConfigurationProducer() {
    super(true);
  }

  @Override
  public boolean isPreferredConfiguration(ConfigurationFromContext self, ConfigurationFromContext other) {
    TestRunner testRunner = getTestRunner(self.getSourceElement());
    return testRunner == CHOOSE_PER_TEST || testRunner == GRADLE;
  }

  @Override
  public boolean shouldReplace(@NotNull ConfigurationFromContext self, @NotNull ConfigurationFromContext other) {
    return getTestRunner(self.getSourceElement()) == GRADLE;
  }

  @Override
  protected boolean setupConfigurationFromContext(@NotNull ExternalSystemRunConfiguration configuration,
                                                  @NotNull ConfigurationContext context,
                                                  @NotNull Ref<PsiElement> sourceElement) {
    if (!GradleConstants.SYSTEM_ID.equals(configuration.getSettings().getExternalSystemId())) return false;

    if (sourceElement.isNull()) return false;
    TestRunner testRunner = getTestRunner(sourceElement.get());
    if (testRunner == PLATFORM) return false;
    if (configuration instanceof GradleRunConfiguration) {
      final GradleRunConfiguration gradleRunConfiguration = (GradleRunConfiguration)configuration;
      gradleRunConfiguration.setScriptDebugEnabled(false);
    }
    return doSetupConfigurationFromContext(configuration, context, sourceElement);
  }

  protected abstract boolean doSetupConfigurationFromContext(ExternalSystemRunConfiguration configuration,
                                                             ConfigurationContext context,
                                                             Ref<PsiElement> sourceElement);

  @Override
  public boolean isConfigurationFromContext(@NotNull ExternalSystemRunConfiguration configuration, @NotNull ConfigurationContext context) {
    if (!GradleConstants.SYSTEM_ID.equals(configuration.getSettings().getExternalSystemId())) return false;

    String projectPath = configuration.getSettings().getExternalProjectPath();
    TestRunner testRunner = getTestRunner(context.getProject(), projectPath);
    if (testRunner == PLATFORM) return false;
    return doIsConfigurationFromContext(configuration, context);
  }

  protected abstract boolean doIsConfigurationFromContext(ExternalSystemRunConfiguration configuration, ConfigurationContext context);

  @Nullable
  protected String resolveProjectPath(@NotNull Module module) {
    return GradleRunnerUtil.resolveProjectPath(module);
  }

  protected TestTasksChooser getTestTasksChooser() {
    return testTasksChooser;
  }

  @TestOnly
  public void setTestTasksChooser(TestTasksChooser testTasksChooser) {
    this.testTasksChooser = testTasksChooser;
  }

  public static boolean hasTasksInConfiguration(VirtualFile source, Project project, ExternalSystemTaskExecutionSettings settings) {
    List<TasksToRun> tasksToRun = findAllTestsTaskToRun(source, project);
    List<List<String>> escapedTasks = ContainerUtil.map(tasksToRun, tasks -> ContainerUtil.map(tasks, it -> escapeIfNeeded(it)));
    List<String> taskNames = settings.getTaskNames();
    if (escapedTasks.stream().anyMatch(taskNames::containsAll)) return true;
    String scriptParameters = settings.getScriptParameters();
    if (StringUtil.isEmpty(scriptParameters)) return false;
    List<String> escapedJoinedTasks = ContainerUtil.map(escapedTasks, it -> StringUtil.join(it, " "));
    return escapedJoinedTasks.stream().anyMatch(scriptParameters::contains);
  }

  /**
   * Finds any of possible tasks to run tests for specified source
   *
   * @param source  is a file or directory for find in source set
   * @param project is a project with the source
   * @return any of possible tasks to run tests for specified source
   */
  @NotNull
  public static TasksToRun findTestsTaskToRun(@NotNull VirtualFile source, @NotNull Project project) {
    List<TasksToRun> tasksToRun = findAllTestsTaskToRun(source, project);
    if (tasksToRun.isEmpty()) return TasksToRun.EMPTY;
    return tasksToRun.get(0);
  }

  /**
   * Finds all of possible tasks to run tests for specified source
   *
   * @param source  is a file or directory for find in source set
   * @param project is a project with the source
   * @return all of possible tasks to run tests for specified source
   */
  @NotNull
  public static List<TasksToRun> findAllTestsTaskToRun(@NotNull VirtualFile source, @NotNull Project project) {
    String sourcePath = source.getPath();
    ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);
    Module module = projectFileIndex.getModuleForFile(source);
    if (module == null) return Collections.emptyList();
    List<TasksToRun> testTasks = new ArrayList<>();
    for (GradleTestTasksProvider provider : GradleTestTasksProvider.EP_NAME.getExtensions()) {
      List<String> tasks = provider.getTasks(module, source);
      if (!ContainerUtil.isEmpty(tasks)) {
        String testName = StringUtil.join(tasks, " ");
        testTasks.add(new TasksToRun.Impl(testName, tasks));
      }
    }
    DataNode<ModuleData> moduleDataNode = GradleUtil.findGradleModuleData(module);
    if (moduleDataNode == null) return testTasks;
    Collection<DataNode<TestData>> testsData = ExternalSystemApiUtil.findAll(moduleDataNode, ProjectKeys.TEST);
    for (DataNode<TestData> testDataNode : testsData) {
      TestData testData = testDataNode.getData();
      Set<String> sourceFolders = testData.getSourceFolders();
      for (String sourceFolder : sourceFolders) {
        if (FileUtil.isAncestor(sourceFolder, sourcePath, false)) {
          String testName = testData.getTestName();
          String testTaskName = testData.getTestTaskName();
          String clearTestTaskName = testData.getCleanTestTaskName();
          List<String> tasks = ContainerUtil.newArrayList(clearTestTaskName, testTaskName);
          testTasks.add(new TasksToRun.Impl(testName, tasks));
        }
      }
    }
    return testTasks;
  }

  @Deprecated
  @NotNull
  public static List<String> getTasksToRun(@NotNull Module module) {
    for (GradleTestTasksProvider provider : GradleTestTasksProvider.EP_NAME.getExtensions()) {
      final List<String> tasks = provider.getTasks(module);
      if(!ContainerUtil.isEmpty(tasks)) {
        return tasks;
      }
    }

    final String externalProjectId = ExternalSystemApiUtil.getExternalProjectId(module);
    if (externalProjectId == null) return ContainerUtil.emptyList();
    final String projectPath = ExternalSystemApiUtil.getExternalProjectPath(module);
    if (projectPath == null) return ContainerUtil.emptyList();
    final ExternalProjectInfo externalProjectInfo =
      getExternalProjectInfo(module.getProject(), GradleConstants.SYSTEM_ID, projectPath);
    if (externalProjectInfo == null) return ContainerUtil.emptyList();

    final List<String> tasks;
    final String gradlePath = GradleProjectResolverUtil.getGradlePath(module);
    if (gradlePath == null) return ContainerUtil.emptyList();
    String taskPrefix = endsWithChar(gradlePath, ':') ? gradlePath : (gradlePath + ':');

    if (StringUtil.endsWith(externalProjectId, ":test") || StringUtil.endsWith(externalProjectId, ":main")) {
      return ContainerUtil.map(TEST_SOURCE_SET_TASKS, task -> taskPrefix + task);
    }

    final DataNode<ModuleData> moduleNode =
      GradleProjectResolverUtil.findModule(externalProjectInfo.getExternalProjectStructure(), projectPath);
    if (moduleNode == null) return ContainerUtil.emptyList();

    final DataNode<TaskData> taskNode;
    final String sourceSetId = StringUtil.substringAfter(externalProjectId, moduleNode.getData().getExternalName() + ':');
    if (sourceSetId == null) {
      taskNode = ExternalSystemApiUtil.find(
        moduleNode, ProjectKeys.TASK,
        node -> node.getData().isTest() &&
                StringUtil.equals("test", node.getData().getName()) || StringUtil.equals(taskPrefix + "test", node.getData().getName()));
    }
    else {
      taskNode = ExternalSystemApiUtil.find(
        moduleNode, ProjectKeys.TASK,
        node -> node.getData().isTest() && StringUtil.startsWith(node.getData().getName(), sourceSetId));
    }

    if (taskNode == null) return ContainerUtil.emptyList();
    String taskName = StringUtil.trimStart(taskNode.getData().getName(), taskPrefix);
    tasks = Arrays.asList("clean" + StringUtil.capitalize(taskName), taskName);
    return ContainerUtil.map(tasks, task -> taskPrefix + task);
  }

  private static TestRunner getTestRunner(@NotNull Project project, @NotNull String projectPath) {
    return GradleProjectSettings.getTestRunner(project, projectPath);
  }

  private static TestRunner getTestRunner(@NotNull PsiElement sourceElement) {
    Module module = ModuleUtilCore.findModuleForPsiElement(sourceElement);
    if (module == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("Cannot find module for %s", sourceElement.toString()), new Throwable());
      }
      return PLATFORM;
    }
    return GradleProjectSettings.getTestRunner(module);
  }
}
