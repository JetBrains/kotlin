// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.task;

import com.google.gson.GsonBuilder;
import com.intellij.build.SyncViewManager;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager;
import com.intellij.openapi.externalSystem.task.TaskCallback;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.SystemProperties;
import com.intellij.util.execution.ParametersListUtil;
import org.gradle.api.Task;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolver;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverExtension;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleBuildParticipant;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper.DISPATCH_PORT_SYS_PROP;
import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState.BUILD_PROCESS_DEBUGGER_PORT_KEY;
import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState.DEBUGGER_DISPATCH_PORT_KEY;
import static com.intellij.util.containers.ContainerUtil.addAllNotNull;
import static com.intellij.util.containers.ContainerUtil.set;
import static org.jetbrains.plugins.gradle.util.GradleUtil.determineRootProject;

/**
 * @author Denis Zhdanov
 */
public class GradleTaskManager implements ExternalSystemTaskManager<GradleExecutionSettings> {

  public static final Key<String> INIT_SCRIPT_KEY = Key.create("INIT_SCRIPT_KEY");
  public static final Key<String> INIT_SCRIPT_PREFIX_KEY = Key.create("INIT_SCRIPT_PREFIX_KEY");
  private static final Logger LOG = Logger.getInstance(GradleTaskManager.class);
  private final GradleExecutionHelper myHelper = new GradleExecutionHelper();

  private final Map<ExternalSystemTaskId, CancellationTokenSource> myCancellationMap = new ConcurrentHashMap<>();

  public GradleTaskManager() {
  }

  @Override
  public void executeTasks(final @NotNull ExternalSystemTaskId id,
                           final @NotNull List<String> taskNames,
                           @NotNull String projectPath,
                           @Nullable GradleExecutionSettings settings,
                           final @Nullable String jvmParametersSetup,
                           final @NotNull ExternalSystemTaskNotificationListener listener) throws ExternalSystemException {
    final List<String> tasks = taskNames.stream()
      .flatMap(s -> ParametersListUtil.parse(s, false, true).stream())
      .collect(Collectors.toList());

    if (ExternalSystemApiUtil.isInProcessMode(GradleConstants.SYSTEM_ID)) {
      for (GradleTaskManagerExtension gradleTaskManagerExtension : GradleTaskManagerExtension.EP_NAME.getExtensions()) {
        if (gradleTaskManagerExtension.executeTasks(id, tasks, projectPath, settings, jvmParametersSetup, listener)) {
          return;
        }
      }
    }

    GradleExecutionSettings effectiveSettings =
      settings == null ? new GradleExecutionSettings(null, null, DistributionType.BUNDLED, false) : settings;

    CancellationTokenSource cancellationTokenSource = GradleConnector.newCancellationTokenSource();
    myCancellationMap.put(id, cancellationTokenSource);
    Function<ProjectConnection, Void> f = connection -> {
      try {
        setupGradleScriptDebugging(effectiveSettings);
        setupDebuggerDispatchPort(effectiveSettings);
        appendInitScriptArgument(tasks, jvmParametersSetup, effectiveSettings);
        try {
          for (GradleBuildParticipant buildParticipant : effectiveSettings.getExecutionWorkspace().getBuildParticipants()) {
            effectiveSettings.withArguments(GradleConstants.INCLUDE_BUILD_CMD_OPTION, buildParticipant.getProjectPath());
          }

          BuildLauncher launcher = myHelper.getBuildLauncher(id, connection, effectiveSettings, listener);
          launcher.forTasks(ArrayUtil.toStringArray(tasks));

          launcher.withCancellationToken(cancellationTokenSource.token());
          launcher.run();
        }
        finally {
          myCancellationMap.remove(id);
        }
        return null;
      }
      catch (RuntimeException e) {
        LOG.debug("Gradle build launcher error", e);
        BuildEnvironment buildEnvironment = GradleExecutionHelper.getBuildEnvironment(connection, id, listener, cancellationTokenSource);
        final GradleProjectResolverExtension projectResolverChain = GradleProjectResolver.createProjectResolverChain();
        throw projectResolverChain.getUserFriendlyError(buildEnvironment, e, projectPath, null);
      }
    };
    if (effectiveSettings.getDistributionType() == DistributionType.WRAPPED) {
      myHelper.ensureInstalledWrapper(id, determineRootProject(projectPath), effectiveSettings, listener, cancellationTokenSource.token());
    }
    myHelper.execute(projectPath, effectiveSettings, id, listener, cancellationTokenSource, f);
  }

  protected static boolean isGradleScriptDebug(@Nullable GradleExecutionSettings settings) {
    return Optional.ofNullable(settings)
      .map(s -> s.getUserData(GradleRunConfiguration.DEBUG_FLAG_KEY))
      .orElse(false);
  }

  @Override
  public boolean cancelTask(@NotNull ExternalSystemTaskId id, @NotNull ExternalSystemTaskNotificationListener listener)
    throws ExternalSystemException {
    final CancellationTokenSource cancellationTokenSource = myCancellationMap.get(id);
    if (cancellationTokenSource != null) {
      cancellationTokenSource.cancel();
      return true;
    }
    // extension points are available only in IDE process
    if (ExternalSystemApiUtil.isInProcessMode(GradleConstants.SYSTEM_ID)) {
      for (GradleTaskManagerExtension gradleTaskManagerExtension : GradleTaskManagerExtension.EP_NAME.getExtensions()) {
        if (gradleTaskManagerExtension.cancelTask(id, listener)) return true;
      }
    }
    return false;
  }

  public static void appendInitScriptArgument(@NotNull List<String> taskNames,
                                              @Nullable String jvmParametersSetup,
                                              @NotNull GradleExecutionSettings effectiveSettings) {
    final List<String> initScripts = new ArrayList<>();
    List<GradleProjectResolverExtension> extensions = GradleProjectResolverUtil.createProjectResolvers(null).collect(Collectors.toList());
    for (GradleProjectResolverExtension resolverExtension : extensions) {
      final String resolverClassName = resolverExtension.getClass().getName();
      Consumer<String> initScriptConsumer = script -> {
        if (StringUtil.isNotEmpty(script)) {
          addAllNotNull(
            initScripts,
            "//-- Generated by " + resolverClassName,
            script,
            "//");
        }
      };

      Map<String, String> enhancementParameters = new HashMap<>();
      enhancementParameters.put(GradleProjectResolverExtension.JVM_PARAMETERS_SETUP_KEY, jvmParametersSetup);

      String isTestExecution = String.valueOf(Boolean.TRUE == effectiveSettings.getUserData(GradleConstants.RUN_TASK_AS_TEST));
      enhancementParameters.put(GradleProjectResolverExtension.TEST_EXECUTION_EXPECTED_KEY, isTestExecution);

      Integer debugDispatchPort = effectiveSettings.getUserData(DEBUGGER_DISPATCH_PORT_KEY);

      if (debugDispatchPort != null) {
        enhancementParameters.put(GradleProjectResolverExtension.DEBUG_DISPATCH_PORT_KEY, String.valueOf(debugDispatchPort));
        String debugOptions = effectiveSettings.getUserData(GradleRunConfiguration.DEBUGGER_PARAMETERS_KEY);
        enhancementParameters.put(GradleProjectResolverExtension.DEBUG_OPTIONS_KEY, debugOptions);
      }


      resolverExtension.enhanceTaskProcessing(taskNames, initScriptConsumer, enhancementParameters);
    }

    if (!initScripts.isEmpty()) {
      try {
        File tempFile = GradleExecutionHelper.writeToFileGradleInitScript(
          StringUtil.join(initScripts, SystemProperties.getLineSeparator()), "ijresolvers");
        effectiveSettings.withArguments(GradleConstants.INIT_SCRIPT_CMD_OPTION, tempFile.getAbsolutePath());
      }
      catch (IOException e) {
        ExternalSystemException systemException = new ExternalSystemException(e);
        systemException.initCause(e);
        throw systemException;
      }
    }

    final String initScript = effectiveSettings.getUserData(INIT_SCRIPT_KEY);
    if (StringUtil.isNotEmpty(initScript)) {
      try {
        String initScriptPrefix = effectiveSettings.getUserData(INIT_SCRIPT_PREFIX_KEY);
        if (StringUtil.isEmpty(initScriptPrefix)) {
          initScriptPrefix = "ijmiscinit";
        }
        else {
          initScriptPrefix = FileUtil.sanitizeFileName(initScriptPrefix);
        }
        File tempFile = GradleExecutionHelper.writeToFileGradleInitScript(initScript, initScriptPrefix);
        effectiveSettings.withArguments(GradleConstants.INIT_SCRIPT_CMD_OPTION, tempFile.getAbsolutePath());
      }
      catch (IOException e) {
        ExternalSystemException externalSystemException = new ExternalSystemException(e);
        externalSystemException.initCause(e);
        throw externalSystemException;
      }
    }
  }

  public static void setupGradleScriptDebugging(@NotNull GradleExecutionSettings effectiveSettings) {
    Integer gradleScriptDebugPort = effectiveSettings.getUserData(BUILD_PROCESS_DEBUGGER_PORT_KEY);
    if (isGradleScriptDebug(effectiveSettings) && gradleScriptDebugPort != null && gradleScriptDebugPort > 0) {
      boolean isJdk9orLater = ExternalSystemJdkUtil.isJdk9orLater(effectiveSettings.getJavaHome());
      String jvmOpt = ForkedDebuggerHelper.JVM_DEBUG_SETUP_PREFIX + (isJdk9orLater ? "127.0.0.1:" : "") + gradleScriptDebugPort;
      effectiveSettings.withVmOption(jvmOpt);
    }
  }

  public static void setupDebuggerDispatchPort(@NotNull GradleExecutionSettings effectiveSettings) {
    Integer dispatchPort = effectiveSettings.getUserData(DEBUGGER_DISPATCH_PORT_KEY);
    if (dispatchPort != null) {
      effectiveSettings.withVmOption(String.format("-D%s=%d", DISPATCH_PORT_SYS_PROP, dispatchPort));
    }
  }

  public static void runCustomTask(@NotNull Project project,
                                   @NotNull String executionName,
                                   @NotNull Class<? extends Task> taskClass,
                                   @NotNull String projectPath,
                                   @NotNull String gradlePath,
                                   @Nullable String taskConfiguration,
                                   @Nullable TaskCallback callback) {
    runCustomTask(project, executionName, taskClass, projectPath, gradlePath, taskConfiguration,
                  ProgressExecutionMode.IN_BACKGROUND_ASYNC, callback);
  }

  public static void runCustomTask(@NotNull Project project,
                                   @NotNull String executionName,
                                   @NotNull Class<? extends Task> taskClass,
                                   @NotNull String projectPath,
                                   @NotNull String gradlePath,
                                   @Nullable String taskConfiguration,
                                   @NotNull ProgressExecutionMode progressExecutionMode,
                                   @Nullable TaskCallback callback) {

    String taskName = taskClass.getSimpleName();
    String paths = GradleExecutionHelper.getToolingExtensionsJarPaths(set(taskClass, GsonBuilder.class, ExternalSystemException.class));
    String initScript = "initscript {\n" +
                        "  dependencies {\n" +
                        "    classpath files(" + paths + ")\n" +
                        "  }\n" +
                        "}\n" +
                        "allprojects {\n" +
                        "  afterEvaluate { project ->\n" +
                        "    if(project.path == '" + gradlePath + "') {\n" +
                        "        def overwrite = project.tasks.findByName('" + taskName + "') != null\n" +
                        "        project.tasks.create(name: '" + taskName + "', overwrite: overwrite, type: " + taskClass.getName() + ") {\n" +
                        StringUtil.notNullize(taskConfiguration) + "\n" +
                        "        }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}\n";
    UserDataHolderBase userData = new UserDataHolderBase();
    userData.putUserData(INIT_SCRIPT_KEY, initScript);
    userData.putUserData(ExternalSystemRunConfiguration.PROGRESS_LISTENER_KEY, SyncViewManager.class);

    String gradleVmOptions = GradleSettings.getInstance(project).getGradleVmOptions();
    ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
    settings.setExecutionName(executionName);
    settings.setExternalProjectPath(projectPath);
    String taskPrefix = gradlePath.endsWith(":") ? gradlePath : gradlePath + ':';
    settings.setTaskNames(Collections.singletonList(taskPrefix + taskName));
    settings.setVmOptions(gradleVmOptions);
    settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());
    ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID, project, GradleConstants.SYSTEM_ID, callback,
                               progressExecutionMode, false, userData);
  }
}
