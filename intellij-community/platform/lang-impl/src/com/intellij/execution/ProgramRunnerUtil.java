// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.impl.RunDialog;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.process.ProcessNotCreatedException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.LayeredIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class ProgramRunnerUtil {
  private static final Logger LOG = Logger.getInstance(ProgramRunnerUtil.class);

  private ProgramRunnerUtil() { }

  @Nullable
  public static ProgramRunner<?> getRunner(@NotNull String executorId, @Nullable RunnerAndConfigurationSettings configuration) {
    return configuration == null ? null : ProgramRunner.getRunner(executorId, configuration.getConfiguration());
  }

  public static void executeConfiguration(@NotNull final ExecutionEnvironment environment, boolean showSettings, boolean assignNewId) {
    executeConfigurationAsync(environment, showSettings, assignNewId, null);
  }

  @NotNull
  public static String getCannotRunOnErrorMessage(@NotNull RunProfile profile, @NotNull ExecutionTarget target) {
    return StringUtil.escapeXmlEntities("Cannot run '" + profile.getName() + "' on '" + target.getDisplayName() + "'");
  }

  public static void executeConfigurationAsync(@NotNull ExecutionEnvironment environment,
                                               boolean showSettings,
                                               boolean assignNewId,
                                               ProgramRunner.Callback callback) {
    if (ExecutorRegistry.getInstance().isStarting(environment)) {
      return;
    }

    RunnerAndConfigurationSettings runnerAndConfigurationSettings = environment.getRunnerAndConfigurationSettings();
    final Project project = environment.getProject();
    ProgramRunner<?> runner = environment.getRunner();

    if (runnerAndConfigurationSettings != null) {
      if (!ExecutionTargetManager.canRun(environment)) {
        ExecutionUtil.handleExecutionError(environment, new ExecutionException(
          getCannotRunOnErrorMessage(environment.getRunProfile(), environment.getExecutionTarget())));
        return;
      }

      if ((!RunManagerImpl.canRunConfiguration(environment) || (showSettings && runnerAndConfigurationSettings.isEditBeforeRun())) &&
          !DumbService.isDumb(project)) {
        if (!RunDialog.editConfiguration(environment, "Edit configuration")) {
          return;
        }

        while (!RunManagerImpl.canRunConfiguration(environment)) {
          String message = "Configuration is still incorrect. Do you want to edit it again?";
          String title = "Change Configuration Settings";
          if (Messages.showYesNoDialog(project, message, title, "Edit", "Continue Anyway", Messages.getErrorIcon()) != Messages.YES) {
            break;
          }
          if (!RunDialog.editConfiguration(environment, "Edit configuration")) {
            return;
          }
        }

        // corresponding runner can be changed after configuration edit
        runner = getRunner(environment.getExecutor().getId(), runnerAndConfigurationSettings);
      }
    }

    try {
      if (runner == null) {
        throw new ExecutionException("Cannot find runner for " + environment.getRunProfile().getName());
      }
      if (!runner.equals(environment.getRunner())) {
        environment = new ExecutionEnvironmentBuilder(environment).runner(runner).build();
      }
      if (assignNewId) {
        environment.assignNewExecutionId();
      }
      if (callback != null) {
        environment.getRunner().execute(environment, callback);
      }
      else {
        environment.getRunner().execute(environment);
      }
    }
    catch (ExecutionException e) {
      handleExecutionError(project, environment, e, runnerAndConfigurationSettings != null ? runnerAndConfigurationSettings.getConfiguration() : null);
    }
  }

  public static void handleExecutionError(Project project,
                                          @NotNull ExecutionEnvironment environment,
                                          Throwable e,
                                          RunProfile configuration) {
    String name = configuration != null ? configuration.getName() : environment.getRunProfile().getName();
    String windowId = ExecutionManager.getInstance(project).getContentManager().getToolWindowIdByEnvironment(environment);
    if (configuration instanceof ConfigurationWithCommandLineShortener && ExecutionUtil.isProcessNotCreated(e)) {
      handleProcessNotStartedError((ConfigurationWithCommandLineShortener)configuration, (ProcessNotCreatedException)e, name, windowId);
    }
    else {
      ExecutionUtil.handleExecutionError(project, windowId, name, e);
    }
  }

  private static void handleProcessNotStartedError(ConfigurationWithCommandLineShortener configuration,
                                                   ExecutionException e,
                                                   String name,
                                                   String windowId) {
    String description = e.getMessage();
    HyperlinkListener listener = null;
    Project project = configuration.getProject();
    RunManager runManager = RunManager.getInstance(project);
    RunnerAndConfigurationSettings runnerAndConfigurationSettings = runManager.getAllSettings().stream()
      .filter(settings -> settings.getConfiguration() == configuration)
      .findFirst()
      .orElse(null);
    if (runnerAndConfigurationSettings != null &&
        (configuration.getShortenCommandLine() == null || configuration.getShortenCommandLine() == ShortenCommandLine.NONE)) {
      ConfigurationFactory factory = runnerAndConfigurationSettings.getFactory();
      RunnerAndConfigurationSettings configurationTemplate = runManager.getConfigurationTemplate(factory);

      description = "Command line is too long. Shorten command line for <a href=\"current\">" + name + "</a>";
      if (((ConfigurationWithCommandLineShortener)configurationTemplate.getConfiguration()).getShortenCommandLine() == null) {
        description += " or also for " + factory.getName() + " <a href=\"default\">default</a> configuration";
      }
      description += ".";

      listener = event -> {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          boolean isDefaultConfigurationChosen = "default".equals(event.getDescription());
          SingleConfigurableEditor dialog = RunDialog.editShortenClasspathSetting(
            isDefaultConfigurationChosen ? configurationTemplate : runnerAndConfigurationSettings,
            "Edit" + (isDefaultConfigurationChosen ? " Default" : "") + " Configuration");
          if (dialog.showAndGet() && isDefaultConfigurationChosen) {
            configuration.setShortenCommandLine(((ConfigurationWithCommandLineShortener)configurationTemplate.getConfiguration()).getShortenCommandLine());
          }
        }
      };
    }
    ExecutionUtil.handleExecutionError(project, windowId, name, e, description, listener);
  }

  /** @deprecated Use {@link #executeConfiguration(RunnerAndConfigurationSettings, Executor)} */
  @Deprecated
  public static void executeConfiguration(@SuppressWarnings("unused") @NotNull Project project,
                                          @NotNull RunnerAndConfigurationSettings configuration,
                                          @NotNull Executor executor) {
    executeConfiguration(configuration, executor);
  }

  public static void executeConfiguration(@NotNull RunnerAndConfigurationSettings configuration, @NotNull Executor executor) {
    ExecutionEnvironmentBuilder builder;
    try {
      builder = ExecutionEnvironmentBuilder.create(executor, configuration);
    }
    catch (ExecutionException e) {
      LOG.error(e);
      return;
    }

    executeConfiguration(builder.contentToReuse(null).dataContext(null).activeTarget().build(), true, true);
  }

  public static Icon getConfigurationIcon(RunnerAndConfigurationSettings settings, boolean invalid) {
    Icon icon = getRawIcon(settings);

    final Icon configurationIcon = settings.isTemporary() ?  getTemporaryIcon(icon): icon;
    if (invalid) {
      return LayeredIcon.create(configurationIcon, AllIcons.RunConfigurations.InvalidConfigurationLayer);
    }

    return configurationIcon;
  }

  @NotNull
  public static Icon getRawIcon(@NotNull RunnerAndConfigurationSettings settings) {
    Icon icon = settings.getFactory().getIcon(settings.getConfiguration());
    return icon == null ? AllIcons.Actions.Help : icon;
  }

  @NotNull
  public static Icon getTemporaryIcon(@NotNull Icon rawIcon) {
    return IconLoader.getTransparentIcon(rawIcon, 0.3f);
  }

  @NotNull
  public static String shortenName(@Nullable String name, final int toBeAdded) {
    if (name == null) {
      return "";
    }

    final int symbols = Math.max(10, 20 - toBeAdded);
    return name.length() < symbols + 3 ? name : name.substring(0, symbols) + "...";
  }
}