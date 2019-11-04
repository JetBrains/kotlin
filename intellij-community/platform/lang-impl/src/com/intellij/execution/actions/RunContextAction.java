// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.actions;

import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.ThreeState;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

import static com.intellij.execution.SuggestUsingRunDashBoardUtil.promptUserToUseRunDashboard;

public class RunContextAction extends BaseRunConfigurationAction {
  private final Executor myExecutor;

  public RunContextAction(@NotNull final Executor executor) {
    super(ExecutionBundle.message("perform.action.with.context.configuration.action.name", executor.getStartActionText()), null, new IconLoader.LazyIcon() {
      @NotNull
      @Override
      protected Icon compute() {
        return executor.getIcon();
      }
    });
    myExecutor = executor;
  }

  @Override
  protected void perform(final ConfigurationContext context) {
    RunnerAndConfigurationSettings configuration = context.findExisting();
    final RunManagerEx runManager = (RunManagerEx)context.getRunManager();
    if (configuration == null) {
      configuration = context.getConfiguration();
      if (configuration == null) {
        return;
      }
      runManager.setTemporaryConfiguration(configuration);
    }
    if (Registry.is("select.run.configuration.from.context")) {
      runManager.setSelectedConfiguration(configuration);
    }

    if (LOG.isDebugEnabled()) {
      String configurationClass = configuration.getConfiguration().getClass().getName();
      LOG.debug(String.format("Execute run configuration: %s", configurationClass));
    }
    ExecutionUtil.doRunConfiguration(configuration, myExecutor, null, null, context.getDataContext());
  }

  @Override
  protected boolean isEnabledFor(RunConfiguration configuration) {
    return getRunner(configuration) != null;
  }

  @Nullable
  private ProgramRunner getRunner(final RunConfiguration configuration) {
    return ProgramRunner.getRunner(myExecutor.getId(), configuration);
  }

  @Override
  protected void updatePresentation(final Presentation presentation, @NotNull final String actionText, final ConfigurationContext context) {
    presentation.setText(myExecutor.getStartActionText(actionText), true);

    Pair<Boolean, Boolean> b = isEnabledAndVisible(context);

    presentation.setEnabled(b.first);
    presentation.setVisible(b.second);
  }

  @Override
  protected void approximatePresentationByPreviousAvailability(AnActionEvent event, ThreeState hadAnythingRunnable) {
    super.approximatePresentationByPreviousAvailability(event, hadAnythingRunnable);
    event.getPresentation().setText(myExecutor.getStartActionText() + "...");
  }

  private Pair<Boolean, Boolean> isEnabledAndVisible(ConfigurationContext context) {
    RunnerAndConfigurationSettings configuration = context.findExisting();
    if (configuration == null) {
      configuration = context.getConfiguration();
    }

    ProgramRunner runner = configuration == null ? null : getRunner(configuration.getConfiguration());
    if (runner == null) {
      return Pair.create(false, false);
    }
    return Pair.create(!ExecutorRegistry.getInstance().isStarting(context.getProject(), myExecutor.getId(), runner.getRunnerId()), true);
  }

  @NotNull
  @Override
  protected List<AnAction> createChildActions(@NotNull ConfigurationContext context,
                                              @NotNull List<? extends ConfigurationFromContext> configurations) {
    final List<AnAction> childActions = new ArrayList<>(super.createChildActions(context, configurations));
    boolean isMultipleConfigurationsFromAlternativeLocations =
      configurations.size() > 1 && configurations.get(0).isFromAlternativeLocation();
    boolean isRunAction = myExecutor.getId().equals(DefaultRunExecutor.EXECUTOR_ID);
    if (isMultipleConfigurationsFromAlternativeLocations && isRunAction) {
      childActions.add(runAllConfigurationsAction(context, configurations));
    }

    return childActions;
  }

  @NotNull
  private AnAction runAllConfigurationsAction(@NotNull ConfigurationContext context, @NotNull List<? extends ConfigurationFromContext> configurationsFromContext) {
    return new AnAction(
      "Run all",
      "Run all configurations available in this context",
      AllIcons.RunConfigurations.Compound
    ) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        long groupId = ExecutionEnvironment.getNextUnusedExecutionId();

        List<ConfigurationType> types = ContainerUtil.map(configurationsFromContext, context1 -> context1.getConfiguration().getType());
        promptUserToUseRunDashboard(context.getProject(), types);

        for (ConfigurationFromContext configuration : configurationsFromContext) {
          ExecutionUtil.runConfiguration(configuration.getConfigurationSettings(), myExecutor, groupId);
        }
      }
    };
  }

}
