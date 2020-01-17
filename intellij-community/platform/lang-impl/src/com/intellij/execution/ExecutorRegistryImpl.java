// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution;

import com.intellij.execution.actions.RunContextAction;
import com.intellij.execution.compound.CompoundRunConfiguration;
import com.intellij.execution.compound.SettingsAndEffectiveTarget;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.executors.ExecutorGroup;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.icons.AllIcons;
import com.intellij.ide.macro.MacroManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionConfigurationCustomizer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.IconUtil;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class ExecutorRegistryImpl extends ExecutorRegistry {
  private static final Logger LOG = Logger.getInstance(ExecutorRegistryImpl.class);

  public static final String RUNNERS_GROUP = "RunnerActions";
  public static final String RUN_CONTEXT_GROUP = "RunContextGroupInner";

  private final Set<String> myContextActionIdSet = new THashSet<>();
  private final Map<String, AnAction> myIdToAction = new THashMap<>();
  private final Map<String, AnAction> myContextActionIdToAction = new THashMap<>();

  public ExecutorRegistryImpl() {
    Executor.EXECUTOR_EXTENSION_NAME.addExtensionPointListener(new ExtensionPointListener<Executor>() {
      @Override
      public void extensionAdded(@NotNull Executor extension, @NotNull PluginDescriptor pluginDescriptor) {
        //noinspection TestOnlyProblems
        initExecutorActions(extension, ActionManager.getInstance());
      }

      @Override
      public void extensionRemoved(@NotNull Executor extension, @NotNull PluginDescriptor pluginDescriptor) {
        deinitExecutor(extension);
      }
    }, ApplicationManager.getApplication());
  }

  final static class ExecutorRegistryActionConfigurationTuner implements ActionConfigurationCustomizer {
    @Override
    public void customize(@NotNull ActionManager manager) {
      if (Executor.EXECUTOR_EXTENSION_NAME.hasAnyExtensions()) {
        ((ExecutorRegistryImpl)getInstance()).init(manager);
      }
    }
  }

  @TestOnly
  public synchronized void initExecutorActions(@NotNull Executor executor, @NotNull ActionManager actionManager) {
    if (myContextActionIdSet.contains(executor.getContextActionId())) {
      LOG.error("Executor with context action id: \"" + executor.getContextActionId() + "\" was already registered!");
    }

    AnAction toolbarAction;
    AnAction runContextAction;
    if (executor instanceof ExecutorGroup) {
      ExecutorGroup<?> executorGroup = (ExecutorGroup<?>)executor;
      ActionGroup toolbarActionGroup = new SplitButtonAction(new ExecutorGroupActionGroup(executorGroup, ExecutorAction::new));
      Presentation presentation = toolbarActionGroup.getTemplatePresentation();
      presentation.setIcon(executor.getIcon());
      presentation.setText(executor.getStartActionText());
      presentation.setDescription(executor.getDescription());
      toolbarAction = toolbarActionGroup;
      runContextAction = new ExecutorGroupActionGroup(executorGroup, RunContextAction::new);
    }
    else {
      toolbarAction = new ExecutorAction(executor);
      runContextAction = new RunContextAction(executor);
    }

    Executor.ActionWrapper customizer = executor.runnerActionsGroupExecutorActionCustomizer();
    registerAction(actionManager, executor.getId(), customizer == null ? toolbarAction : customizer.wrap(toolbarAction), RUNNERS_GROUP, myIdToAction);
    registerAction(actionManager, executor.getContextActionId(), runContextAction, RUN_CONTEXT_GROUP, myContextActionIdToAction);

    myContextActionIdSet.add(executor.getContextActionId());
  }

  private static void registerAction(@NotNull ActionManager actionManager, @NotNull String actionId, @NotNull AnAction anAction, @NotNull String groupId, @NotNull Map<String, AnAction> map) {
    AnAction action = actionManager.getAction(actionId);
    if (action == null) {
      actionManager.registerAction(actionId, anAction);
      map.put(actionId, anAction);
      action = anAction;
    }

    ((DefaultActionGroup)actionManager.getAction(groupId)).add(action, actionManager);
  }

  synchronized void deinitExecutor(@NotNull Executor executor) {
    myContextActionIdSet.remove(executor.getContextActionId());

    unregisterAction(executor.getId(), RUNNERS_GROUP, myIdToAction);
    unregisterAction(executor.getContextActionId(), RUN_CONTEXT_GROUP, myContextActionIdToAction);
  }

  private static void unregisterAction(@NotNull String actionId, @NotNull String groupId, @NotNull Map<String, AnAction> map) {
    ActionManager actionManager = ActionManager.getInstance();
    DefaultActionGroup group = (DefaultActionGroup)actionManager.getAction(groupId);
    if (group == null) {
      return;
    }

    group.remove(actionManager.getAction(actionId), actionManager);
    AnAction action = map.get(actionId);
    if (action != null) {
      actionManager.unregisterAction(actionId);
      map.remove(actionId);
    }
  }

  @Override
  public Executor getExecutorById(@NotNull String executorId) {
    // even IJ Ultimate with all plugins has ~7 executors - linear search is ok here
    for (Executor executor : Executor.EXECUTOR_EXTENSION_NAME.getExtensionList()) {
      if (executorId.equals(executor.getId())) {
        return executor;
      }
    }
    return null;
  }

  private void init(@NotNull ActionManager actionManager) {
    for (Executor executor : Executor.EXECUTOR_EXTENSION_NAME.getExtensionList()) {
      try {
        //noinspection TestOnlyProblems
        initExecutorActions(executor, actionManager);
      }
      catch (Throwable t) {
        LOG.error("executor initialization failed: " + executor.getClass().getName(), t);
      }
    }
  }

  private static final class ExecutorAction extends AnAction implements DumbAware, UpdateInBackground {
    private final Executor myExecutor;

    private ExecutorAction(@NotNull Executor executor) {
      super(executor.getStartActionText(), executor.getDescription(), IconLoader.createLazy(() -> executor.getIcon()));
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
            || ExecutionManager.getInstance(project).isStarting(myExecutor.getId(), runner.getRunnerId())) {
          return false;
        }
      }
      return true;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      Presentation presentation = e.getPresentation();
      Project project = e.getProject();
      if (project == null || !project.isInitialized() || project.isDisposed()) {
        presentation.setEnabled(false);
        return;
      }

      RunnerAndConfigurationSettings selectedSettings = getSelectedConfiguration(project);
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

    private Icon getInformativeIcon(@NotNull Project project, @NotNull RunnerAndConfigurationSettings selectedConfiguration) {
      ExecutionManagerImpl executionManager = ExecutionManagerImpl.getInstance(project);
      RunConfiguration configuration = selectedConfiguration.getConfiguration();
      if (configuration instanceof RunnerIconProvider) {
        RunnerIconProvider provider = (RunnerIconProvider)configuration;
        Icon icon = provider.getExecutorIcon(configuration, myExecutor);
        if (icon != null) {
          return icon;
        }
      }

      List<RunContentDescriptor> runningDescriptors = executionManager.getRunningDescriptors(s -> {
        return s != null && s.getConfiguration() == selectedConfiguration.getConfiguration();
      });
      runningDescriptors = ContainerUtil.filter(runningDescriptors, descriptor -> {
        RunContentDescriptor contentDescriptor = RunContentManager.getInstance(project).findContentDescriptor(myExecutor, descriptor.getProcessHandler());
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
        return IconUtil.addText(myExecutor.getIcon(), Integer.toString(runningDescriptors.size()));
      }
    }

    @Nullable
    private static RunnerAndConfigurationSettings getSelectedConfiguration(@NotNull Project project) {
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
    public void actionPerformed(@NotNull AnActionEvent e) {
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
  public final static class ExecutorGroupActionGroup extends ActionGroup implements DumbAware {
    private final ExecutorGroup<?> myExecutorGroup;
    private final Function<? super Executor, ? extends AnAction> myChildConverter;

    private ExecutorGroupActionGroup(@NotNull ExecutorGroup<?> executorGroup, @NotNull Function<? super Executor, ? extends AnAction> childConverter) {
      myExecutorGroup = executorGroup;
      myChildConverter = childConverter;
    }

    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
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
