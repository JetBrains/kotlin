// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution;

import com.intellij.execution.actions.RunContextAction;
import com.intellij.execution.compound.CompoundRunConfiguration;
import com.intellij.execution.compound.SettingsAndEffectiveTarget;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.executors.ExecutorGroup;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.ide.macro.MacroManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.*;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Trinity;
import com.intellij.util.IconUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.function.Function;

public final class ExecutorRegistryImpl extends ExecutorRegistry implements Disposable {
  private static final Logger LOG = Logger.getInstance(ExecutorRegistryImpl.class);

  public static final String RUNNERS_GROUP = "RunnerActions";
  public static final String RUN_CONTEXT_GROUP = "RunContextGroupInner";

  private List<Executor> myExecutors = new ArrayList<>();
  private final Map<String, Executor> myIdToExecutor = new THashMap<>();
  private final Set<String> myContextActionIdSet = new THashSet<>();
  private final Map<String, AnAction> myIdToAction = new THashMap<>();
  private final Map<String, AnAction> myContextActionIdToAction = new THashMap<>();

  // [Project, ExecutorId, RunnerId]
  private final Set<Trinity<Project, String, String>> myInProgress = Collections.synchronizedSet(new THashSet<>());

  public ExecutorRegistryImpl() {
    init();
  }

  static class ExecutorRegistryPreloader extends PreloadingActivity {
    @Override
    public void preload(@NotNull ProgressIndicator indicator) {
      getInstance();
    }
  }

  synchronized void initExecutor(@NotNull Executor executor) {
    if (myIdToExecutor.get(executor.getId()) != null) {
      LOG.error("Executor with id: \"" + executor.getId() + "\" was already registered!");
    }

    if (myContextActionIdSet.contains(executor.getContextActionId())) {
      LOG.error("Executor with context action id: \"" + executor.getContextActionId() + "\" was already registered!");
    }

    final AnAction toolbarAction;
    final AnAction runContextAction;
    if (executor instanceof ExecutorGroup) {
      ActionGroup toolbarActionGroup = new SplitButtonAction(new ExecutorGroupActionGroup((ExecutorGroup<?>)executor, ExecutorAction::new));
      final Presentation presentation = toolbarActionGroup.getTemplatePresentation();
      presentation.setIcon(executor.getIcon());
      presentation.setText(executor.getStartActionText());
      presentation.setDescription(executor.getDescription());
      toolbarAction = toolbarActionGroup;
      runContextAction = new ExecutorGroupActionGroup((ExecutorGroup<?>)executor, RunContextAction::new);
    }
    else {
      toolbarAction = new ExecutorAction(executor);
      runContextAction = new RunContextAction(executor);
    }
    final Executor.ActionWrapper customizer = executor.runnerActionsGroupExecutorActionCustomizer();
    registerAction(executor.getId(), customizer != null ? customizer.wrap(toolbarAction) : toolbarAction, RUNNERS_GROUP, myIdToAction);
    registerAction(executor.getContextActionId(), runContextAction, RUN_CONTEXT_GROUP, myContextActionIdToAction);

    myExecutors.add(executor);
    myIdToExecutor.put(executor.getId(), executor);
    myContextActionIdSet.add(executor.getContextActionId());
  }

  private static void registerAction(@NotNull String actionId, @NotNull AnAction anAction, @NotNull String groupId, @NotNull Map<String, AnAction> map) {
    ActionManager actionManager = ActionManager.getInstance();
    AnAction action = actionManager.getAction(actionId);
    if (action == null) {
      actionManager.registerAction(actionId, anAction);
      map.put(actionId, anAction);
      action = anAction;
    }

    ((DefaultActionGroup)actionManager.getAction(groupId)).add(action);
  }

  synchronized void deinitExecutor(@NotNull Executor executor) {
    myExecutors.remove(executor);
    myIdToExecutor.remove(executor.getId());
    myContextActionIdSet.remove(executor.getContextActionId());

    unregisterAction(executor.getId(), RUNNERS_GROUP, myIdToAction);
    unregisterAction(executor.getContextActionId(), RUN_CONTEXT_GROUP, myContextActionIdToAction);
  }

  private static void unregisterAction(@NotNull String actionId, @NotNull String groupId, @NotNull Map<String, AnAction> map) {
    ActionManager actionManager = ActionManager.getInstance();
    final DefaultActionGroup group = (DefaultActionGroup)actionManager.getAction(groupId);
    if (group != null) {
      group.remove(actionManager.getAction(actionId), actionManager);
      final AnAction action = map.get(actionId);
      if (action != null) {
        actionManager.unregisterAction(actionId);
        map.remove(actionId);
      }
    }
  }

  @Override
  @NotNull
  public synchronized Executor[] getRegisteredExecutors() {
    return myExecutors.toArray(new Executor[0]);
  }

  @Override
  public Executor getExecutorById(final String executorId) {
    return myIdToExecutor.get(executorId);
  }

  private void init() {
    MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(this);
    connection.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
      @Override
      public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment environment) {
        myInProgress.add(createExecutionId(executorId, environment));
      }

      @Override
      public void processNotStarted(@NotNull String executorId, @NotNull ExecutionEnvironment environment) {
        myInProgress.remove(createExecutionId(executorId, environment));
      }

      @Override
      public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment environment, @NotNull ProcessHandler handler) {
        myInProgress.remove(createExecutionId(executorId, environment));
      }
    });
    connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectClosed(@NotNull final Project project) {
        // perform cleanup
        synchronized (myInProgress) {
          for (Iterator<Trinity<Project, String, String>> it = myInProgress.iterator(); it.hasNext(); ) {
            if (project == it.next().first) {
              it.remove();
            }
          }
        }
      }
    });

    for (Executor executor : Executor.EXECUTOR_EXTENSION_NAME.getExtensionList()) {
      try {
        initExecutor(executor);
      }
      catch (Throwable t) {
        LOG.error("executor initialization failed: " + executor.getClass().getName(), t);
      }
    }
  }

  @NotNull
  private static Trinity<Project, String, String> createExecutionId(String executorId, @NotNull ExecutionEnvironment environment) {
    return Trinity.create(environment.getProject(), executorId, environment.getRunner().getRunnerId());
  }

  @Override
  public boolean isStarting(Project project, String executorId, String runnerId) {
    return myInProgress.contains(Trinity.create(project, executorId, runnerId));
  }

  @Override
  public boolean isStarting(@NotNull ExecutionEnvironment environment) {
    return isStarting(environment.getProject(), environment.getExecutor().getId(), environment.getRunner().getRunnerId());
  }

  @Override
  public synchronized void dispose() {
    if (!myExecutors.isEmpty()) {
      for (Executor executor : new ArrayList<>(myExecutors)) {
        deinitExecutor(executor);
      }
    }
    myExecutors = null;
  }

  private class ExecutorAction extends AnAction implements DumbAware, UpdateInBackground {
    private final Executor myExecutor;

    private ExecutorAction(@NotNull final Executor executor) {
      super(executor.getStartActionText(), executor.getDescription(), new IconLoader.LazyIcon() {
        @NotNull
        @Override
        protected Icon compute() {
          return executor.getIcon();
        }
      });
      myExecutor = executor;
    }

    private boolean canRun(@NotNull Project project, @NotNull List<SettingsAndEffectiveTarget> pairs) {
      if (pairs.isEmpty()) {
        return false;
      }
      for (SettingsAndEffectiveTarget pair : pairs) {
        RunConfiguration configuration = pair.getConfiguration();
        if (configuration instanceof CompoundRunConfiguration) {
          if (!canRun(project, ((CompoundRunConfiguration)configuration).getConfigurationsWithEffectiveRunTargets())) {
            return false;
          }
          continue;
        }
        ProgramRunner<?> runner = ProgramRunner.getRunner(myExecutor.getId(), configuration);
        if (runner == null
            || !ExecutionTargetManager.canRun(configuration, pair.getTarget())
            || isStarting(project, myExecutor.getId(), runner.getRunnerId())) {
          return false;
        }
      }
      return true;
    }

    @Override
    public void update(@NotNull final AnActionEvent e) {
      final Presentation presentation = e.getPresentation();
      final Project project = e.getProject();

      if (project == null || !project.isInitialized() || project.isDisposed()) {
        presentation.setEnabled(false);
        return;
      }

      final RunnerAndConfigurationSettings selectedSettings = getSelectedConfiguration(project);
      boolean enabled = false;
      boolean hideDisabledExecutorButtons = false;
      String text;
      if (selectedSettings != null) {
        if (DumbService.isDumb(project) && !selectedSettings.getType().isDumbAware()) {
          presentation.setEnabled(false);
          return;
        }

        presentation.setIcon(getInformativeIcon(project, selectedSettings));
        RunConfiguration configuration = selectedSettings.getConfiguration();
        if (configuration instanceof CompoundRunConfiguration) {
          enabled = canRun(project, ((CompoundRunConfiguration)configuration).getConfigurationsWithEffectiveRunTargets());
        }
        else {
          ExecutionTarget target = ExecutionTargetManager.getActiveTarget(project);
          enabled = canRun(project, Collections.singletonList(new SettingsAndEffectiveTarget(configuration, target)));
          hideDisabledExecutorButtons = configuration.hideDisabledExecutorButtons();
        }
        if (enabled) {
          presentation.setDescription(myExecutor.getDescription());
        }
        text = myExecutor.getStartActionText(configuration.getName());
      }
      else {
        text = getTemplatePresentation().getTextWithMnemonic();
      }

      if (hideDisabledExecutorButtons) {
        presentation.setEnabledAndVisible(enabled);
      }
      else {
        presentation.setEnabled(enabled);
      }

      if (presentation.isVisible()) {
        presentation.setVisible(myExecutor.isApplicable(project));
      }
      presentation.setText(text);
    }

    private Icon getInformativeIcon(Project project, final RunnerAndConfigurationSettings selectedConfiguration) {
      final ExecutionManagerImpl executionManager = ExecutionManagerImpl.getInstance(project);

      RunConfiguration configuration = selectedConfiguration.getConfiguration();
      if (configuration instanceof RunnerIconProvider) {
        RunnerIconProvider provider = (RunnerIconProvider)configuration;
        Icon icon = provider.getExecutorIcon(configuration, myExecutor);
        if (icon != null) {
          return icon;
        }
      }

      List<RunContentDescriptor> runningDescriptors =
        executionManager.getRunningDescriptors(s -> s != null && s.getConfiguration() == selectedConfiguration.getConfiguration());
      runningDescriptors = ContainerUtil.filter(runningDescriptors, descriptor -> {
        RunContentDescriptor contentDescriptor =
          executionManager.getContentManager().findContentDescriptor(myExecutor, descriptor.getProcessHandler());
        return contentDescriptor != null && executionManager.getExecutors(contentDescriptor).contains(myExecutor);
      });

      if (!configuration.isAllowRunningInParallel() && !runningDescriptors.isEmpty() && DefaultRunExecutor.EXECUTOR_ID.equals(myExecutor.getId())) {
        return AllIcons.Actions.Restart;
      }
      if (runningDescriptors.isEmpty()) {
        return myExecutor.getIcon();
      }

      if (runningDescriptors.size() == 1) {
        return ExecutionUtil.getLiveIndicator(myExecutor.getIcon());
      }
      else {
        return IconUtil.addText(myExecutor.getIcon(), String.valueOf(runningDescriptors.size()));
      }
    }

    @Nullable
    private RunnerAndConfigurationSettings getSelectedConfiguration(@NotNull final Project project) {
      return RunManager.getInstance(project).getSelectedConfiguration();
    }

    private void run(@NotNull Project project, @Nullable RunConfiguration configuration, @Nullable RunnerAndConfigurationSettings settings, @NotNull DataContext dataContext) {
      if (configuration instanceof CompoundRunConfiguration) {
        RunManager runManager = RunManager.getInstance(project);
        for (SettingsAndEffectiveTarget settingsAndEffectiveTarget : ((CompoundRunConfiguration)configuration).getConfigurationsWithEffectiveRunTargets()) {
          RunConfiguration subConfiguration = settingsAndEffectiveTarget.getConfiguration();
          run(project, subConfiguration, runManager.findSettings(subConfiguration), dataContext);
        }
      }
      else {
        ExecutionEnvironmentBuilder builder = settings == null ? null : ExecutionEnvironmentBuilder.createOrNull(myExecutor, settings);
        if (builder == null) {
          return;
        }
        ExecutionManager.getInstance(project).restartRunProfile(builder.activeTarget().dataContext(dataContext).build());
      }
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent e) {
      final Project project = e.getProject();
      if (project == null || project.isDisposed()) {
        return;
      }

      MacroManager.getInstance().cacheMacrosPreview(e.getDataContext());
      RunnerAndConfigurationSettings selectedConfiguration = getSelectedConfiguration(project);
      if (selectedConfiguration != null) {
        run(project, selectedConfiguration.getConfiguration(), selectedConfiguration, e.getDataContext());
      }
    }
  }

  // TODO: make private as soon as IDEA-207986 will be fixed
  // RunExecutorSettings configurations can be modified, so we request current childExecutors on each AnAction#update call
  public static class ExecutorGroupActionGroup extends ActionGroup implements DumbAware {
    private final ExecutorGroup<?> myExecutorGroup;
    private final Function<? super Executor, ? extends AnAction> myChildConverter;

    private ExecutorGroupActionGroup(ExecutorGroup<?> executorGroup, Function<? super Executor, ? extends AnAction> childConverter) {
      myExecutorGroup = executorGroup;
      myChildConverter = childConverter;
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      List<Executor> childExecutors = myExecutorGroup.childExecutors();
      AnAction[] result = new AnAction[childExecutors.size()];
      for (int i = 0; i < childExecutors.size(); i++) {
        result[i] = myChildConverter.apply(childExecutors.get(i));
      }
      return result;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      final Project project = e.getProject();
      if (project == null || !project.isInitialized() || project.isDisposed()) {
        e.getPresentation().setEnabled(false);
        return;
      }
      e.getPresentation().setEnabledAndVisible(myExecutorGroup.isApplicable(project));
    }
  }
}