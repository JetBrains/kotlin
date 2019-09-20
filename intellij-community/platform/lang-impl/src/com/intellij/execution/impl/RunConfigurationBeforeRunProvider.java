// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.*;
import com.intellij.execution.compound.ConfigurationSelectionUtil;
import com.intellij.execution.compound.TypeNameTarget;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Vassiliy Kudryashov
 */
public class RunConfigurationBeforeRunProvider
extends BeforeRunTaskProvider<RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask> {
  public static final Key<RunConfigurableBeforeRunTask> ID = Key.create("RunConfigurationTask");

  private static final Logger LOG = Logger.getInstance(RunConfigurationBeforeRunProvider.class);

  private final Project myProject;

  public RunConfigurationBeforeRunProvider(Project project) {
    myProject = project;
  }

  @Override
  public Key<RunConfigurableBeforeRunTask> getId() {
    return ID;
  }

  @Override
  public Icon getIcon() {
    return AllIcons.Actions.Execute;
  }

  @Override
  public Icon getTaskIcon(RunConfigurableBeforeRunTask task) {
    if (task.getSettings() == null)
      return null;
    return ProgramRunnerUtil.getConfigurationIcon(task.getSettings(), false);
  }

  @Override
  public String getName() {
    return ExecutionBundle.message("before.launch.run.another.configuration");
  }

  @Override
  public String getDescription(RunConfigurableBeforeRunTask task) {
    Pair<RunnerAndConfigurationSettings, ExecutionTarget> settingsWithTarget = task.getSettingsWithTarget();
    if (settingsWithTarget == null) {
      return ExecutionBundle.message("before.launch.run.another.configuration");
    }
    else {
      String text = ConfigurationSelectionUtil.getDisplayText(settingsWithTarget.first.getConfiguration(), settingsWithTarget.second);
      return ExecutionBundle.message("before.launch.run.certain.configuration", text);
    }
  }

  @Override
  public boolean isConfigurable() {
    return true;
  }

  @Override
  @Nullable
  public RunConfigurableBeforeRunTask createTask(@NotNull RunConfiguration runConfiguration) {
    return new RunConfigurableBeforeRunTask();
  }

  @Override
  public Promise<Boolean> configureTask(@NotNull DataContext context,
                                        @NotNull RunConfiguration configuration,
                                        @NotNull RunConfigurableBeforeRunTask task) {
    Project project = configuration.getProject();
    RunManagerImpl runManager = RunManagerImpl.getInstanceImpl(project);

    List<RunConfiguration> configurations = ContainerUtil.map(getAvailableConfigurations(configuration), it -> it.getConfiguration());

    AsyncPromise<Boolean> result = new AsyncPromise<>();
    ConfigurationSelectionUtil.createPopup(project, runManager, configurations, (selectedConfigs, selectedTarget) -> {
      RunConfiguration selectedConfig = ContainerUtil.getFirstItem(selectedConfigs);
      RunnerAndConfigurationSettings selectedSettings = selectedConfig == null ? null : runManager.getSettings(selectedConfig);

      if (selectedSettings != null) {
        task.setSettingsWithTarget(selectedSettings, selectedTarget);
        result.setResult(true);
      }
      else {
        result.setResult(false);
      }
    }).showInBestPositionFor(context);

    return result;
  }

  @NotNull
  private static List<RunnerAndConfigurationSettings> getAvailableConfigurations(@NotNull RunConfiguration runConfiguration) {
    Project project = runConfiguration.getProject();
    if (project == null || !project.isInitialized()) {
      return Collections.emptyList();
    }

    List<RunnerAndConfigurationSettings> configurations = new ArrayList<>(RunManagerImpl.getInstanceImpl(project).getAllSettings());
    String executorId = DefaultRunExecutor.getRunExecutorInstance().getId();
    for (Iterator<RunnerAndConfigurationSettings> iterator = configurations.iterator(); iterator.hasNext();) {
      RunnerAndConfigurationSettings settings = iterator.next();
      ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, settings);
      if (runner == null || settings.getConfiguration() == runConfiguration) {
        iterator.remove();
      }
    }
    return configurations;
  }

  @Override
  public boolean canExecuteTask(@NotNull RunConfiguration configuration,
                                @NotNull RunConfigurableBeforeRunTask task) {
    RunnerAndConfigurationSettings settings = task.getSettings();
    if (settings == null) {
      return false;
    }
    String executorId = DefaultRunExecutor.getRunExecutorInstance().getId();
    final ProgramRunner runner = ProgramRunnerUtil.getRunner(executorId, settings);
    return runner != null && runner.canRun(executorId, settings.getConfiguration());
  }

  @Override
  public boolean executeTask(@NotNull final DataContext dataContext,
                             @NotNull RunConfiguration configuration,
                             @NotNull final ExecutionEnvironment env,
                             @NotNull RunConfigurableBeforeRunTask task) {
    Pair<RunnerAndConfigurationSettings, ExecutionTarget> settings = task.getSettingsWithTarget();
    if (settings == null) {
      return true; // ignore missing configurations: IDEA-155476 Run/debug silently fails when 'Run another configuration' step is broken
    }
    return doExecuteTask(env, settings.first, settings.second);
  }

  public static boolean doExecuteTask(@NotNull final ExecutionEnvironment env,
                                      @NotNull final RunnerAndConfigurationSettings settings,
                                      @Nullable final ExecutionTarget target) {
    RunConfiguration configuration = settings.getConfiguration();
    Executor executor = configuration instanceof BeforeRunTaskAwareConfiguration &&
                        ((BeforeRunTaskAwareConfiguration)configuration).useRunExecutor()
                        ? DefaultRunExecutor.getRunExecutorInstance()
                        : env.getExecutor();
    final String executorId = executor.getId();
    ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.createOrNull(executor, settings);
    if (builder == null) {
      return false;
    }

    ExecutionTarget effectiveTarget = target;

    if (effectiveTarget == null && ExecutionTargetManager.canRun(settings.getConfiguration(), env.getExecutionTarget())) {
      effectiveTarget = env.getExecutionTarget();
    }

    List<ExecutionTarget> allTargets = ExecutionTargetManager.getInstance(env.getProject()).getTargetsFor(settings.getConfiguration());
    if (effectiveTarget == null) {
      effectiveTarget = ContainerUtil.find(allTargets, it -> it.isReady());
    }
    if (effectiveTarget == null) {
      effectiveTarget = ContainerUtil.getFirstItem(allTargets);
    }

    if (effectiveTarget == null) {
      return false;
    }

    final ExecutionEnvironment environment = builder.target(effectiveTarget).build();
    environment.setExecutionId(env.getExecutionId());

    if (!environment.getRunner().canRun(executorId, environment.getRunProfile())) {
      return false;
    }
    else {
      beforeRun(environment);
      return doRunTask(executorId, environment, environment.getRunner());
    }
  }

  public static boolean doRunTask(final String executorId, final ExecutionEnvironment environment, ProgramRunner<?> runner) {
    final Semaphore targetDone = new Semaphore();
    final Ref<Boolean> result = new Ref<>(false);
    final Disposable disposable = Disposer.newDisposable();

    environment.getProject().getMessageBus().connect(disposable).subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
      @Override
      public void processStartScheduled(@NotNull final String executorIdLocal, @NotNull final ExecutionEnvironment environmentLocal) {
        if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
          targetDone.down();
        }
      }

      @Override
      public void processNotStarted(@NotNull final String executorIdLocal, @NotNull final ExecutionEnvironment environmentLocal) {
        if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
          Boolean skipRun = environment.getUserData(ExecutionManagerImpl.EXECUTION_SKIP_RUN);
          if (skipRun != null && skipRun) {
            result.set(true);
          }
          targetDone.up();
        }
      }

      @Override
      public void processTerminated(@NotNull String executorIdLocal,
                                    @NotNull ExecutionEnvironment environmentLocal,
                                    @NotNull ProcessHandler handler,
                                    int exitCode) {
        if (executorId.equals(executorIdLocal) && environment.equals(environmentLocal)) {
          result.set(exitCode == 0);
          targetDone.up();
        }
      }
    });

    try {
      ApplicationManager.getApplication().invokeAndWait(() -> {
        try {
          runner.execute(environment);
        }
        catch (ExecutionException e) {
          targetDone.up();
          LOG.error(e);
        }
      }, ModalityState.NON_MODAL);
    }
    catch (Exception e) {
      LOG.error(e);
      Disposer.dispose(disposable);
      return false;
    }

    targetDone.waitFor();
    Disposer.dispose(disposable);

    return result.get();
  }

  private static void beforeRun(@NotNull ExecutionEnvironment environment) {
    for (RunConfigurationBeforeRunProviderDelegate delegate : RunConfigurationBeforeRunProviderDelegate.EP_NAME.getExtensionList()) {
      delegate.beforeRun(environment);
    }
  }

  public class RunConfigurableBeforeRunTask extends BeforeRunTask<RunConfigurableBeforeRunTask> {
    private @Nullable TypeNameTarget myTypeNameTarget;

    private @Nullable Pair</*@NotNull*/RunnerAndConfigurationSettings, /*@Nullable*/ExecutionTarget> mySettingsWithTarget;

    RunConfigurableBeforeRunTask() {
      super(ID);
    }

    @Override
    public void writeExternal(@NotNull Element element) {
      super.writeExternal(element);
      if (myTypeNameTarget != null) {
        element.setAttribute("run_configuration_name", myTypeNameTarget.getName());
        element.setAttribute("run_configuration_type", myTypeNameTarget.getType());
        if (myTypeNameTarget.getTargetId() != null) {
          element.setAttribute("run_configuration_target", myTypeNameTarget.getTargetId());
        }
      }
    }

    @Override
    public void readExternal(@NotNull Element element) {
      super.readExternal(element);

      String name = element.getAttributeValue("run_configuration_name");
      String type = element.getAttributeValue("run_configuration_type");
      String targetId = element.getAttributeValue("run_configuration_target");
      if (name != null && type != null) myTypeNameTarget = new TypeNameTarget(type, name, targetId);

      mySettingsWithTarget = null;
    }

    // avoid RunManagerImpl.getInstanceImpl and findConfigurationByTypeAndName calls (can be called during RunManagerImpl initialization)
    boolean isMySettings(@NotNull RunnerAndConfigurationSettings settings) {
      if (mySettingsWithTarget != null) {
        // instance equality
        return mySettingsWithTarget.first == settings;
      }

      return myTypeNameTarget != null
             && settings.getType().getId().equals(myTypeNameTarget.getType())
             && settings.getName().equals(myTypeNameTarget.getName());
    }

    void init() {
      if (mySettingsWithTarget != null) {
        return;
      }

      if (myTypeNameTarget != null) {
        RunnerAndConfigurationSettings settings = RunManagerImpl.getInstanceImpl(myProject).findConfigurationByTypeAndName(
          myTypeNameTarget.getType(), myTypeNameTarget.getName());
        ExecutionTarget target = ((ExecutionTargetManagerImpl)ExecutionTargetManager.getInstance(myProject)).findTargetByIdFor(
          settings != null ? settings.getConfiguration() : null, myTypeNameTarget.getTargetId());

        setSettingsWithTarget(settings, target);
      }
    }

    public void setSettingsWithTarget(@Nullable RunnerAndConfigurationSettings settings, @Nullable ExecutionTarget target) {
      mySettingsWithTarget = settings == null ? null : Pair.create(settings, target);
      myTypeNameTarget = settings == null ? null : new TypeNameTarget(settings.getType().getId(), settings.getName(),
                                                                      target == null ? null : target.getId());
    }

    @Nullable
    public RunnerAndConfigurationSettings getSettings() {
      return Pair.getFirst(getSettingsWithTarget());
    }

    @Nullable
    public Pair</*NotNull*/ RunnerAndConfigurationSettings, /*Nullable*/ExecutionTarget> getSettingsWithTarget() {
      init();
      return mySettingsWithTarget;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      RunConfigurableBeforeRunTask that = (RunConfigurableBeforeRunTask)o;

      return Comparing.equal(myTypeNameTarget, that.myTypeNameTarget);
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (myTypeNameTarget != null ? myTypeNameTarget.hashCode() : 0);
      return result;
    }
  }
}