// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ex.IdeFocusTraversalPolicy;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.event.ActionEvent;
import java.util.List;

public class RunDialog extends DialogWrapper implements RunDialogBase {
  private final Project myProject;
  private final RunConfigurable myConfigurable;
  private JComponent myCenterPanel;
  private final Executor myExecutor;

  public RunDialog(final Project project, final Executor executor) {
    super(project, true);
    myProject = project;
    myExecutor = executor;

    setTitle(executor.getId());

    setOKButtonText(executor.getActionName());
    //setOKButtonIcon(executor.getIcon());

    myConfigurable = new ProjectRunConfigurationConfigurable(project, this);
    init();
    myConfigurable.reset();
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), new ApplyAction(), getHelpAction()};
  }

  @Override
  protected String getHelpId() {
    return "reference.dialogs.rundebug";
  }

  @Override
  protected String getDimensionServiceKey(){
    return "#com.intellij.execution.impl.RunDialog";
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myCenterPanel);
  }

  @Override
  protected void doOKAction() {
    try {
      myConfigurable.apply();
    }
    catch (ConfigurationException e) {
      Messages.showMessageDialog(myProject, e.getMessage(), ExecutionBundle.message("invalid.data.dialog.title"), Messages.getErrorIcon());
      return;
    }
    super.doOKAction();
  }

  @Override
  protected JComponent createCenterPanel() {
    myCenterPanel = myConfigurable.createComponent();
    return myCenterPanel;
  }

  @Override
  protected void dispose() {
    myConfigurable.disposeUIResources();
    super.dispose();
  }

  public static boolean editConfiguration(final Project project, @NotNull RunnerAndConfigurationSettings configuration, final String title) {
    return editConfiguration(project, configuration, title, null);
  }

  public static boolean editConfiguration(@NotNull ExecutionEnvironment environment, @NotNull String title) {
    //noinspection ConstantConditions
    return editConfiguration(environment.getProject(), environment.getRunnerAndConfigurationSettings(), title, environment.getExecutor());
  }

  public static boolean editConfiguration(final Project project, @NotNull RunnerAndConfigurationSettings configuration, final String title, @Nullable final Executor executor) {
    SingleConfigurationConfigurable<RunConfiguration> configurable = SingleConfigurationConfigurable.editSettings(configuration, executor);
    final SingleConfigurableEditor dialog = new SingleConfigurableEditor(project, configurable, IdeModalityType.IDE) {
      {
        if (executor != null) {
          setOKButtonText(executor.getActionName());
          //setOKButtonIcon(executor.getIcon());
        }
      }
    };

    dialog.setTitle(title);
    return dialog.showAndGet();
  }

  public static SingleConfigurableEditor editShortenClasspathSetting(final RunnerAndConfigurationSettings settings, final String title) {
    SingleConfigurationConfigurable<RunConfiguration> configurable = SingleConfigurationConfigurable.editSettings(settings, null);
    final SingleConfigurableEditor dialog = new SingleConfigurableEditor(settings.getConfiguration().getProject(), configurable, IdeModalityType.IDE) {
      @Override
      public JComponent getPreferredFocusedComponent() {
        List<LabeledComponent> labeledComponents = UIUtil.findComponentsOfType(getContentPanel(), LabeledComponent.class);
        String shortenLabelText = ExecutionBundle.message("application.configuration.shorten.command.line.label").replace("\u001B", "");
        for (LabeledComponent component : labeledComponents) {
          if (shortenLabelText.equals(component.getRawText())) {
           JComponent cp = component.getComponent();
            if (cp instanceof ComboBox) {
              ApplicationManager.getApplication().invokeLater(() -> {
                ComboPopup popup = ((ComboBox)cp).getPopup();
                if (popup != null && cp.isShowing()) {
                  popup.show();
                }
              });
              return cp;
            }
          }
        }
        return super.getPreferredFocusedComponent();
      }
    };
    dialog.setTitle(title);
    return dialog;
  }

  private class ApplyAction extends AbstractAction {
    ApplyAction() {
      super(ExecutionBundle.message("apply.action.name"));
    }

    @Override
    public void actionPerformed(final ActionEvent event) {
      try{
        myConfigurable.apply();
      }
      catch(ConfigurationException e){
        Messages.showMessageDialog(myProject, e.getMessage(), ExecutionBundle.message("invalid.data.dialog.title"), Messages.getErrorIcon());
      }
    }
  }

  @Override
  public Executor getExecutor() {
    return myExecutor;
  }
}