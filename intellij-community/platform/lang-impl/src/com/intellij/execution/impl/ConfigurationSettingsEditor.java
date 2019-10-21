// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.execution.impl;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.AdjustingTabSettingsEditor;
import com.intellij.openapi.options.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 * @author dyoma
 */
public class ConfigurationSettingsEditor extends CompositeSettingsEditor<RunnerAndConfigurationSettings> {
  private final ArrayList<SettingsEditor<RunnerAndConfigurationSettings>> myRunnerEditors = new ArrayList<>();
  private final Map<ProgramRunner, List<SettingsEditor>> myRunner2UnwrappedEditors = new HashMap<>();
  private RunnersEditorComponent myRunnersComponent;
  private final RunConfiguration myConfiguration;
  private final SettingsEditor<RunConfiguration> myConfigurationEditor;
  private SettingsEditorGroup<RunnerAndConfigurationSettings> myCompound;

  private static final String RUNNERS_TAB_NAME = ExecutionBundle.message("run.configuration.startup.connection.rab.title");

  private GroupSettingsBuilder<RunnerAndConfigurationSettings> myGroupSettingsBuilder;

  @Override
  public CompositeSettingsBuilder<RunnerAndConfigurationSettings> getBuilder() {
    init();
    myGroupSettingsBuilder = new GroupSettingsBuilder<>(myCompound);
    return myGroupSettingsBuilder;
  }

  private void init() {
    if (myCompound == null) {
      myCompound = new SettingsEditorGroup<>();
      Disposer.register(this, myCompound);
      if (myConfigurationEditor instanceof SettingsEditorGroup) {
        SettingsEditorGroup<RunConfiguration> group = (SettingsEditorGroup<RunConfiguration>)myConfigurationEditor;
        List<Pair<String, SettingsEditor<RunConfiguration>>> editors = group.getEditors();
        for (Pair<String, SettingsEditor<RunConfiguration>> pair : editors) {
          myCompound.addEditor(pair.getFirst(), new ConfigToSettingsWrapper(pair.getSecond()));
        }
      }
      else {
        myCompound.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"),
                             new ConfigToSettingsWrapper(myConfigurationEditor));
      }


      myRunnersComponent = new RunnersEditorComponent();

      for (final Executor executor : ExecutorRegistry.getInstance().getRegisteredExecutors()) {
        ProgramRunner<RunnerSettings> runner = ProgramRunner.getRunner(executor.getId(), myConfiguration);
        if (runner != null) {
          JComponent perRunnerSettings = createCompositePerRunnerSettings(executor, runner);
          if (perRunnerSettings != null) {
            myRunnersComponent.addExecutorComponent(executor, perRunnerSettings);
          }
        }
      }

      if (myRunnerEditors.size() > 0) {
        myCompound.addEditor(RUNNERS_TAB_NAME,
                             new CompositeSettingsEditor<RunnerAndConfigurationSettings>(getFactory()) {
                               @Override
                               public CompositeSettingsBuilder<RunnerAndConfigurationSettings> getBuilder() {
                                 return new CompositeSettingsBuilder<RunnerAndConfigurationSettings>() {
                                   @Override
                                   public Collection<SettingsEditor<RunnerAndConfigurationSettings>> getEditors() {
                                     return myRunnerEditors;
                                   }

                                   @Override
                                   public JComponent createCompoundEditor() {
                                     return myRunnersComponent.getComponent();
                                   }
                                 };
                               }
                             });
      }
    }
  }

  @Nullable
  private JComponent createCompositePerRunnerSettings(@NotNull Executor executor, @NotNull ProgramRunner<RunnerSettings> runner) {
    final SettingsEditor<ConfigurationPerRunnerSettings> configEditor = myConfiguration.getRunnerSettingsEditor(runner);
    SettingsEditor<RunnerSettings> runnerEditor;
    try {
      runnerEditor = runner.getSettingsEditor(executor, myConfiguration);
    }
    catch (AbstractMethodError error) {
      // this is stub code for plugin compatibility
      runnerEditor = null;
    }

    if (configEditor == null && runnerEditor == null) {
      return null;
    }

    SettingsEditor<RunnerAndConfigurationSettings> wrappedConfigEditor = null;
    SettingsEditor<RunnerAndConfigurationSettings> wrappedRunEditor = null;
    if (configEditor != null) {
      wrappedConfigEditor = wrapEditor(configEditor,
                                       runner, configurationSettings -> configurationSettings.getConfigurationSettings(runner)
      );
    }

    if (runnerEditor != null) {
      wrappedRunEditor = wrapEditor(runnerEditor,
                                    runner, configurationSettings -> configurationSettings.getRunnerSettings(runner)
      );
    }

    if (wrappedRunEditor != null && wrappedConfigEditor != null) {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(wrappedConfigEditor.getComponent(), BorderLayout.CENTER);
      JComponent wrappedRunEditorComponent = wrappedRunEditor.getComponent();
      wrappedRunEditorComponent.setBorder(JBUI.Borders.emptyTop(3));
      panel.add(wrappedRunEditorComponent, BorderLayout.SOUTH);
      return panel;
    }

    return wrappedRunEditor == null ? Objects.requireNonNull(wrappedConfigEditor).getComponent() : wrappedRunEditor.getComponent();
  }

  private <T> SettingsEditor<RunnerAndConfigurationSettings> wrapEditor(SettingsEditor<T> editor,
                                                                        ProgramRunner runner,
                                                                        Function<? super RunnerAndConfigurationSettings, ? extends T> convertor) {
    SettingsEditor<RunnerAndConfigurationSettings> wrappedEditor = new SettingsEditorWrapper<>(editor, convertor);

    List<SettingsEditor> unwrappedEditors = myRunner2UnwrappedEditors.get(runner);
    if (unwrappedEditors == null) {
      unwrappedEditors = new ArrayList<>();
      myRunner2UnwrappedEditors.put(runner, unwrappedEditors);
    }
    unwrappedEditors.add(editor);

    myRunnerEditors.add(wrappedEditor);
    Disposer.register(this, wrappedEditor);

    return wrappedEditor;
  }

  public <T extends SettingsEditor> T selectExecutorAndGetEditor(final ProgramRunner runner, Class<T> editorClass) {
    myGroupSettingsBuilder.selectEditor(RUNNERS_TAB_NAME);
    Executor executor = ContainerUtil.find(myRunnersComponent.getExecutors(),
                                           executor1 -> runner.equals(ProgramRunner.getRunner(executor1.getId(), myConfiguration)));
    if (executor == null) {
      return null;
    }
    myRunnersComponent.selectExecutor(executor);
    return ContainerUtil.findInstance(myRunner2UnwrappedEditors.get(runner), editorClass);
  }

  public <T extends SettingsEditor> T selectTabAndGetEditor(Class<T> editorClass) {
    for (Pair<String, SettingsEditor<RunnerAndConfigurationSettings>> name2editor : myCompound.getEditors()) {
      SettingsEditor<RunnerAndConfigurationSettings> editor = name2editor.getSecond();
      if (editor instanceof ConfigToSettingsWrapper) {
        SettingsEditor<RunConfiguration> configEditor = ((ConfigToSettingsWrapper)editor).getConfigEditor();
        if (editorClass.isInstance(configEditor)) {
          myGroupSettingsBuilder.selectEditor(name2editor.getFirst());
          return editorClass.cast(configEditor);
        }
      }
    }
    return null;
  }

  public ConfigurationSettingsEditor(@NotNull RunnerAndConfigurationSettings settings) {
    super(settings.createFactory());

    myConfigurationEditor = (SettingsEditor<RunConfiguration>)settings.getConfiguration().getConfigurationEditor();
    Disposer.register(this, myConfigurationEditor);
    myConfiguration = settings.getConfiguration();
  }

  @NotNull
  @Override
  public RunnerAndConfigurationSettings getSnapshot() throws ConfigurationException {
    RunnerAndConfigurationSettings settings = getFactory().create();
    settings.setName(myConfiguration.getName());
    if (myConfigurationEditor instanceof CheckableRunConfigurationEditor) {
      ((CheckableRunConfigurationEditor)myConfigurationEditor).checkEditorData(settings.getConfiguration());
    }
    else {
      applyTo(settings);
    }
    return settings;
  }

  private static final class RunnersEditorComponent {
    @NonNls private static final String NO_RUNNER_COMPONENT = "<NO RUNNER LABEL>";

    private JList<Executor> myRunnersList;
    private JPanel myRunnerPanel;
    private final CardLayout myLayout = new CardLayout();
    private final DefaultListModel<Executor> myListModel = new DefaultListModel<>();
    private final JLabel myNoRunner = new JLabel(ExecutionBundle.message("run.configuration.norunner.selected.label"));
    private JPanel myRunnersPanel;
    private JBScrollPane myScrollPane;

    RunnersEditorComponent() {
      myRunnerPanel.setLayout(myLayout);
      myRunnerPanel.add(myNoRunner, NO_RUNNER_COMPONENT);
      myRunnersList.setModel(myListModel);
      myRunnersList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          updateRunnerComponent();
        }
      });
      updateRunnerComponent();
      myRunnersList.setCellRenderer(SimpleListCellRenderer.create((label, value, index) -> {
        label.setIcon(value.getIcon());
        label.setText(value.getId());
      }));
      myScrollPane.setBorder(JBUI.Borders.empty());
      myScrollPane.setViewportBorder(JBUI.Borders.empty());
    }

    private void updateRunnerComponent() {
      Executor executor = myRunnersList.getSelectedValue();
      myLayout.show(myRunnerPanel, executor != null ? executor.getId() : NO_RUNNER_COMPONENT);
      myRunnersPanel.revalidate();
    }

    public void addExecutorComponent(Executor executor, JComponent component) {
      myRunnerPanel.add(component, executor.getId());
      myListModel.addElement(executor);
      ScrollingUtil.ensureSelectionExists(myRunnersList);
    }

    public List<Executor> getExecutors() {
      return Collections.list(myListModel.elements());
    }

    public void selectExecutor(Executor executor) {
      myRunnersList.setSelectedValue(executor, true);
    }

    public JComponent getComponent() {
      return myRunnersPanel;
    }
  }

  private class ConfigToSettingsWrapper extends SettingsEditor<RunnerAndConfigurationSettings> {
    private final SettingsEditor<RunConfiguration> myConfigEditor;

    ConfigToSettingsWrapper(SettingsEditor<RunConfiguration> configEditor) {
      myConfigEditor = configEditor;
      if (configEditor instanceof RunConfigurationSettingsEditor) {
        ((RunConfigurationSettingsEditor)configEditor).setOwner(ConfigurationSettingsEditor.this);
      }
    }

    public SettingsEditor<RunConfiguration> getConfigEditor() {
      return myConfigEditor;
    }

    @Override
    public void resetEditorFrom(@NotNull RunnerAndConfigurationSettings configurationSettings) {
      myConfigEditor.resetFrom(configurationSettings.getConfiguration());
    }

    @Override
    public void applyEditorTo(@NotNull RunnerAndConfigurationSettings configurationSettings) throws ConfigurationException {
      myConfigEditor.applyTo(configurationSettings.getConfiguration());
    }

    @Override
    @NotNull
    public JComponent createEditor() {
      JComponent component = myConfigEditor.getComponent();
      if (myConfigEditor instanceof AdjustingTabSettingsEditor) {
        JPanel panel = new JPanel(new BorderLayout());
        UiNotifyConnector connector = new UiNotifyConnector(panel, new Activatable() {
          private boolean myIsEmpty = true;
          @Override
          public void showNotify() {
            if (myIsEmpty) {
              panel.add(component, BorderLayout.CENTER);
              panel.revalidate();
              panel.repaint();
              myIsEmpty = false;
            }
          }

          @Override
          public void hideNotify() {
            if (!myIsEmpty) {
              panel.removeAll();
              panel.revalidate();
              panel.repaint();
              myIsEmpty = true;
            }
          }

        });
        Disposer.register(this, connector);
        return panel;
      }
      return component;
    }

    @Override
    public void disposeEditor() {
      Disposer.dispose(myConfigEditor);
    }
  }
}
