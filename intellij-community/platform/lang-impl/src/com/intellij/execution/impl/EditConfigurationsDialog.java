// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class EditConfigurationsDialog extends SingleConfigurableEditor implements RunDialogBase {
  protected Executor myExecutor;

  public EditConfigurationsDialog(@NotNull Project project) {
    this(project, null);
  }

  public EditConfigurationsDialog(@NotNull Project project, @Nullable ConfigurationFactory factory) {
    super(project, RunConfigurableKt.createRunConfigurationConfigurable(project).selectConfigurableOnShow(factory == null), "#com.intellij.execution.impl.EditConfigurationsDialog", IdeModalityType.IDE);

    ((RunConfigurable)getConfigurable()).setRunDialog(this);
    setTitle(ExecutionBundle.message("run.debug.dialog.title"));
    setHorizontalStretch(1.3F);
    if (factory != null) {
      addRunConfiguration(factory);
    }
  }

  private void addRunConfiguration(@NotNull final ConfigurationFactory factory) {
    final RunConfigurable configurable = (RunConfigurable)getConfigurable();
    final SingleConfigurationConfigurable<RunConfiguration> configuration = configurable.createNewConfiguration(factory);

    if (!isVisible()) {
       getContentPanel().addComponentListener(new ComponentAdapter() {
         @Override
         public void componentShown(ComponentEvent e) {
           configurable.updateRightPanel(configuration);
           getContentPanel().removeComponentListener(this);
         }
       });
    }
  }

  @Override
  protected void doOKAction() {
    RunConfigurable configurable = (RunConfigurable)getConfigurable();
    super.doOKAction();
    if (isOK()) {
      // if configurable was not modified, apply was not called and Run Configurable has not called 'updateActiveConfigurationFromSelected'
      configurable.updateActiveConfigurationFromSelected();
    }
  }

  @Nullable
  @Override
  public Executor getExecutor() {
    return myExecutor;
  }
}