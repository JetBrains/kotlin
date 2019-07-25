// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.compound;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunConfigurationBeforeRunProvider;
import com.intellij.execution.impl.RunConfigurationSelector;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunManagerImplKt;
import com.intellij.ide.DataManager;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompoundRunConfigurationSettingsEditor extends SettingsEditor<CompoundRunConfiguration> {
  private final Project myProject;
  private final JBList<Pair<RunConfiguration, ExecutionTarget>> myList;
  private final RunManagerImpl myRunManager;
  private final SortedListModel<Pair<RunConfiguration, ExecutionTarget>> myModel;
  private CompoundRunConfiguration mySnapshot;

  public CompoundRunConfigurationSettingsEditor(@NotNull Project project) {
    myProject = project;
    myRunManager = RunManagerImpl.getInstanceImpl(project);
    myModel = new SortedListModel<>((o1, o2) -> CompoundRunConfiguration.COMPARATOR.compare(o1.first, o2.first));
    myList = new JBList<>(myModel);
    myList.setCellRenderer(SimpleListCellRenderer.create((label, value, index) -> {
      label.setIcon(value.first.getType().getIcon());
      label.setText(ConfigurationSelectionUtil.getDisplayText(value.first, value.second));
    }));
    myList.setVisibleRowCount(15);
  }


  private boolean canBeAdded(@NotNull RunConfiguration candidate, @NotNull final CompoundRunConfiguration root) {
    if (candidate.getType() == root.getType() && candidate.getName().equals(root.getName())) return false;
    List<BeforeRunTask<?>> tasks = RunManagerImplKt.doGetBeforeRunTasks(candidate);
    for (BeforeRunTask task : tasks) {
      if (task instanceof RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask) {
        RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask runTask
          = (RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask)task;
        RunnerAndConfigurationSettings settings = runTask.getSettings();
        if (settings != null) {
         if (!canBeAdded(settings.getConfiguration(), root)) return false;
        }
      }
    }
    if (candidate instanceof CompoundRunConfiguration) {
      for (RunConfiguration configuration : ((CompoundRunConfiguration)candidate).getConfigurationsWithTargets(myRunManager).keySet()) {
        if (!canBeAdded(configuration, root)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  protected void resetEditorFrom(@NotNull CompoundRunConfiguration compoundRunConfiguration) {
    myModel.clear();
    myModel.addAll(ContainerUtil.map2List(compoundRunConfiguration.getConfigurationsWithTargets(myRunManager)));
    mySnapshot = compoundRunConfiguration;
  }

  @Override
  protected void applyEditorTo(@NotNull CompoundRunConfiguration compoundConfiguration) throws ConfigurationException {
    Map<RunConfiguration, ExecutionTarget> checked = new THashMap<>();
    for (int i = 0; i < myModel.getSize(); i++) {
      Pair<RunConfiguration, ExecutionTarget> configurationAndTarget = myModel.get(i);
      RunConfiguration configuration = configurationAndTarget.first;
      String message =
          LangBundle.message("compound.run.configuration.cycle", configuration.getType().getDisplayName(), configuration.getName());
        if (!canBeAdded(configuration, compoundConfiguration)) {
          throw new ConfigurationException(message);
        }

        checked.put(configuration, configurationAndTarget.second);
    }
    compoundConfiguration.setConfigurationsWithTargets(checked);
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myList);
    return decorator.disableUpDownActions().setAddAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        List<RunConfiguration> configurations = new ArrayList<>();
        for (RunnerAndConfigurationSettings settings : myRunManager.getAllSettings()) {
          RunConfiguration configuration = settings.getConfiguration();
          if (!mySnapshot.getConfigurationsWithTargets(myRunManager).keySet().contains(configuration) && canBeAdded(configuration, mySnapshot)) {
            configurations.add(configuration);
          }
        }

        ConfigurationSelectionUtil.createPopup(myProject, myRunManager, configurations, (selectedConfigs, selectedTarget) -> {
          for (RunConfiguration each : selectedConfigs) {
            myModel.add(Pair.create(each, selectedTarget));
          }
        }).showUnderneathOf(decorator.getActionsPanel());
      }
    }).setEditAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        int index = myList.getSelectedIndex();
        if (index == -1) return;
        RunConfiguration configuration = myModel.get(index).first;
        RunConfigurationSelector selector =
          RunConfigurationSelector.KEY.getData(DataManager.getInstance().getDataContext(button.getContextComponent()));
        if (selector != null) {
          selector.select(configuration);
        }
      }
    }).setToolbarPosition(ActionToolbarPosition.TOP).createPanel();
  }
}
