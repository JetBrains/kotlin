// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.UnknownRunConfiguration;
import com.intellij.execution.impl.RunConfigurationBeforeRunProvider.RunConfigurableBeforeRunTask;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.SmartList;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

/**
 * @author Vassiliy Kudryashov
 */
final class BeforeRunStepsPanel extends JPanel {
  private final JCheckBox myShowSettingsBeforeRunCheckBox;
  private final JCheckBox myActivateToolWindowBeforeRunCheckBox;
  private final JBList<BeforeRunTask<?>> myList;
  private final CollectionListModel<BeforeRunTask<?>> myModel;
  private RunConfiguration myRunConfiguration;

  private final List<BeforeRunTask<?>> originalTasks = new SmartList<>();
  private final StepsBeforeRunListener myListener;
  private final JPanel myPanel;

  private final Set<BeforeRunTask<?>> clonedTasks = new THashSet<>();

  BeforeRunStepsPanel(@NotNull StepsBeforeRunListener listener) {
    myListener = listener;
    myModel = new CollectionListModel<>();
    myList = new JBList<>(myModel);
    myList.getEmptyText().setText(ExecutionBundle.message("before.launch.panel.empty"));
    myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myList.setCellRenderer(new MyListCellRenderer());
    myList.setVisibleRowCount(4);

    myModel.addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        updateText();
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
        updateText();
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
      }
    });

    ToolbarDecorator myDecorator = ToolbarDecorator.createDecorator(myList);
    if (!SystemInfo.isMac) {
      myDecorator.setAsUsualTopToolbar();
    }

    myDecorator.setEditAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        BeforeRunTaskAndProvider selection = getSelection();
        if (selection == null) {
          return;
        }

        BeforeRunTask<?> task = selection.getTask();
        if (!clonedTasks.contains(task)) {
          task = task.clone();
          clonedTasks.add(task);
          myModel.setElementAt(task, selection.getIndex());
        }

        selection.getProvider().configureTask(button.getDataContext(), myRunConfiguration, task)
          .onSuccess(changed -> {
            if (changed) {
              updateText();
            }
          });
      }
    });
    //noinspection Convert2Lambda
    myDecorator.setEditActionUpdater(new AnActionButtonUpdater() {
      @Override
      public boolean isEnabled(@NotNull AnActionEvent e) {
        BeforeRunTaskAndProvider selection = getSelection();
        return selection != null && selection.getProvider().isConfigurable();
      }
    });
    myDecorator.setAddAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        doAddAction(button);
      }
    });
    //noinspection Convert2Lambda
    myDecorator.setAddActionUpdater(new AnActionButtonUpdater() {
      @Override
      public boolean isEnabled(@NotNull AnActionEvent e) {
        return checkBeforeRunTasksAbility(true);
      }
    });

    myShowSettingsBeforeRunCheckBox = new JCheckBox(ExecutionBundle.message("configuration.edit.before.run"));
    myShowSettingsBeforeRunCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateText();
      }
    });
    myActivateToolWindowBeforeRunCheckBox = new JCheckBox(ExecutionBundle.message("configuration.activate.toolwindow.before.run"));
    myActivateToolWindowBeforeRunCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateText();
      }
    });

    myPanel = myDecorator.createPanel();
    myDecorator.getActionsPanel().setCustomShortcuts(CommonActionsPanel.Buttons.EDIT,
                                                     CommonActionsPanel.getCommonShortcut(CommonActionsPanel.Buttons.EDIT),
                                                     CommonShortcuts.DOUBLE_CLICK_1);


    setLayout(new BorderLayout());
    add(myPanel, BorderLayout.CENTER);
    JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, JBUIScale.scale(5), JBUIScale.scale(5)));
    checkboxPanel.add(myShowSettingsBeforeRunCheckBox);
    checkboxPanel.add(myActivateToolWindowBeforeRunCheckBox);
    add(checkboxPanel, BorderLayout.SOUTH);
  }

  @Nullable
  private BeforeRunTaskAndProvider getSelection() {
    final int index = myList.getSelectedIndex();
    if (index == -1) {
      return null;
    }
    BeforeRunTask<?> task = myModel.getElementAt(index);
    @SuppressWarnings("unchecked")
    BeforeRunTaskProvider<BeforeRunTask<?>> provider = BeforeRunTaskProvider.getProvider(myRunConfiguration.getProject(), (Key)task.getProviderId());
    return provider == null ? null : new BeforeRunTaskAndProvider(task, provider, index);
  }

  void doReset(@NotNull RunnerAndConfigurationSettings settings) {
    clonedTasks.clear();

    myRunConfiguration = settings.getConfiguration();

    originalTasks.clear();
    originalTasks.addAll(RunManagerImplKt.doGetBeforeRunTasks(myRunConfiguration));
    myModel.replaceAll(originalTasks);
    myShowSettingsBeforeRunCheckBox.setSelected(settings.isEditBeforeRun());
    myShowSettingsBeforeRunCheckBox.setEnabled(!isUnknown());
    myActivateToolWindowBeforeRunCheckBox.setSelected(settings.isActivateToolWindowBeforeRun());
    myActivateToolWindowBeforeRunCheckBox.setEnabled(!isUnknown());
    myPanel.setVisible(checkBeforeRunTasksAbility(false));
    updateText();
  }

  private void updateText() {
    StringBuilder sb = new StringBuilder();

    if (myShowSettingsBeforeRunCheckBox.isSelected()) {
      sb.append(ExecutionBundle.message("configuration.edit.before.run"));
    }

    List<BeforeRunTask<?>> tasks = myModel.getItems();
    if (!tasks.isEmpty()) {
      LinkedHashMap<BeforeRunTaskProvider<?>, Integer> counter = new LinkedHashMap<>();
      for (BeforeRunTask<?> task : tasks) {
        //noinspection unchecked
        BeforeRunTaskProvider<BeforeRunTask> provider = BeforeRunTaskProvider.getProvider(myRunConfiguration.getProject(), (Key<BeforeRunTask>)task.getProviderId());
        if (provider != null) {
          Integer count = counter.get(provider);
          if (count == null) {
            count = task.getItemsCount();
          }
          else {
            count += task.getItemsCount();
          }
          counter.put(provider, count);
        }
      }
      for (Map.Entry<BeforeRunTaskProvider<?>, Integer> entry : counter.entrySet()) {
        BeforeRunTaskProvider provider = entry.getKey();
        String name = provider.getName();
        name = StringUtil.trimStart(name, "Run ");
        if (sb.length() > 0) {
          sb.append(", ");
        }
        sb.append(name);
        if (entry.getValue() > 1) {
          sb.append(" (").append(entry.getValue().intValue()).append(")");
        }
      }
    }

    if (myActivateToolWindowBeforeRunCheckBox.isSelected()) {
      sb.append(sb.length() > 0 ? ", " : "").append(ExecutionBundle.message("configuration.activate.toolwindow.before.run"));
    }
    if (sb.length() > 0) {
      sb.insert(0, ": ");
    }
    sb.insert(0, ExecutionBundle.message("before.launch.panel.title"));
    myListener.titleChanged(sb.toString());
  }

  @NotNull
  public List<BeforeRunTask<?>> getTasks() {
    List<BeforeRunTask<?>> items = myModel.getItems();
    return items.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(items);
  }

  public boolean needEditBeforeRun() {
    return myShowSettingsBeforeRunCheckBox.isSelected();
  }

  public boolean needActivateToolWindowBeforeRun() {
    return myActivateToolWindowBeforeRunCheckBox.isSelected();
  }

  private boolean checkBeforeRunTasksAbility(boolean checkOnlyAddAction) {
    if (isUnknown()) {
      return false;
    }

    Set<Key> activeProviderKeys = getActiveProviderKeys();
    for (final BeforeRunTaskProvider<BeforeRunTask> provider : getBeforeRunTaskProviders()) {
      if (provider.createTask(myRunConfiguration) != null) {
        if (!checkOnlyAddAction) {
          return true;
        }
        else if (!provider.isSingleton() || !activeProviderKeys.contains(provider.getId())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isUnknown() {
    return myRunConfiguration instanceof UnknownRunConfiguration;
  }

  private void doAddAction(@NotNull AnActionButton button) {
    if (isUnknown()) {
      return;
    }

    Set<Key> activeProviderKeys = getActiveProviderKeys();
    DefaultActionGroup actionGroup = new DefaultActionGroup(null, false);
    for (final BeforeRunTaskProvider<BeforeRunTask> provider : getBeforeRunTaskProviders()) {
      if (provider.createTask(myRunConfiguration) == null || activeProviderKeys.contains(provider.getId()) && provider.isSingleton()) {
        continue;
      }

      actionGroup.add(new AnAction(provider.getName(), null, provider.getIcon()) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          BeforeRunTask task = provider.createTask(myRunConfiguration);
          if (task == null) {
            return;
          }

          provider.configureTask(button.getDataContext(), myRunConfiguration, task)
            .onSuccess(changed -> {
              if (!provider.canExecuteTask(myRunConfiguration, task)) {
                return;
              }
              task.setEnabled(true);

              Set<RunConfiguration> configurationSet = new THashSet<>();
              getAllRunBeforeRuns(task, configurationSet);
              if (configurationSet.contains(myRunConfiguration)) {
                JOptionPane.showMessageDialog(BeforeRunStepsPanel.this,
                                              ExecutionBundle.message("before.launch.panel.cyclic_dependency_warning",
                                                                      myRunConfiguration.getName(),
                                                                      provider.getDescription(task)),
                                              ExecutionBundle.message("warning.common.title"), JOptionPane.WARNING_MESSAGE);
                return;
              }
              addTask(task);
              myListener.fireStepsBeforeRunChanged();
            });
        }
      });
    }
    ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(ExecutionBundle.message("add.new.run.configuration.action2.name"), actionGroup,
                                                                          SimpleDataContext.getProjectContext(myRunConfiguration.getProject()), false, false, false, null,
                                                                          -1, Conditions.alwaysTrue());
    popup.show(Objects.requireNonNull(button.getPreferredPopupPoint()));
  }

  @NotNull
  private List<BeforeRunTaskProvider<BeforeRunTask>> getBeforeRunTaskProviders() {
    return BeforeRunTaskProvider.EXTENSION_POINT_NAME.getExtensionList(myRunConfiguration.getProject());
  }

  public void addTask(@NotNull BeforeRunTask task) {
    myModel.add(task);
  }

  @NotNull
  private Set<Key> getActiveProviderKeys() {
    Set<Key> result = new THashSet<>();
    for (BeforeRunTask task : myModel.getItems()) {
      result.add(task.getProviderId());
    }
    return result;
  }

  private void getAllRunBeforeRuns(@NotNull BeforeRunTask task, @NotNull Set<? super RunConfiguration> configurationSet) {
    if (task instanceof RunConfigurableBeforeRunTask) {
      RunConfiguration configuration = Objects.requireNonNull(((RunConfigurableBeforeRunTask)task).getSettings()).getConfiguration();
      for (BeforeRunTask beforeRunTask : RunManagerImplKt.doGetBeforeRunTasks(configuration)) {
        if (beforeRunTask instanceof RunConfigurableBeforeRunTask) {
          if (configurationSet.add(Objects.requireNonNull(((RunConfigurableBeforeRunTask)beforeRunTask).getSettings()).getConfiguration())) {
            getAllRunBeforeRuns(beforeRunTask, configurationSet);
          }
        }
      }
    }
  }

  interface StepsBeforeRunListener {
    void fireStepsBeforeRunChanged();

    void titleChanged(@NotNull String title);
  }

  private class MyListCellRenderer extends JBList.StripedListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value instanceof BeforeRunTask) {
        BeforeRunTask task = (BeforeRunTask)value;
        @SuppressWarnings("unchecked")
        BeforeRunTaskProvider<BeforeRunTask> provider = BeforeRunTaskProvider.getProvider(myRunConfiguration.getProject(), task.getProviderId());
        if (provider != null) {
          Icon icon = provider.getTaskIcon(task);
          setIcon(icon != null ? icon : provider.getIcon());
          setText(provider.getDescription(task));
        }
      }
      return this;
    }
  }
}
