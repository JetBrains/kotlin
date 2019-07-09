// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.execution.actions;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.RunDialog;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CreateAction extends BaseRunConfigurationAction {
  public CreateAction() {
    super(ExecutionBundle.message("create.run.configuration.action.name"), null, null);
  }

  @Override
  protected void perform(final ConfigurationContext context) {
    choosePolicy(context).perform(context);
  }

  @Override
  protected void updatePresentation(final Presentation presentation, @NotNull final String actionText, final ConfigurationContext context) {
    choosePolicy(context).update(presentation, context, actionText);
  }

  private static BaseCreatePolicy choosePolicy(final ConfigurationContext context) {
    final RunnerAndConfigurationSettings configuration = context.findExisting();
    return configuration == null ? CREATE_AND_EDIT : EDIT;
  }

  private static abstract class BaseCreatePolicy {
    public void update(final Presentation presentation, final ConfigurationContext context, @NotNull final String actionText) {
      updateText(presentation, actionText);
      updateIcon(presentation, context);
    }

    protected void updateIcon(final Presentation presentation, final ConfigurationContext context) {
      final List<ConfigurationFromContext> fromContext = context.getConfigurationsFromContext();
      if (fromContext != null && fromContext.size() == 1) {
        //hide fuzzy icon when multiple run configurations are possible
        presentation.setIcon(fromContext.iterator().next().getConfiguration().getIcon());
      }
    }

    protected abstract void updateText(final Presentation presentation, final String actionText);

    public abstract void perform(ConfigurationContext context);
  }

  private abstract static class CreatePolicy extends BaseCreatePolicy {
    @Override
    public void perform(final ConfigurationContext context) {
      RunManagerImpl runManager = (RunManagerImpl)context.getRunManager();
      RunnerAndConfigurationSettings configuration = context.getConfiguration();
      configuration.setShared(runManager.getConfigurationTemplate(configuration.getFactory()).isShared());
      runManager.addConfiguration(configuration);
      runManager.setSelectedConfiguration(configuration);
    }
  }

  private static class CreateAndEditPolicy extends CreatePolicy {
    @Override
    protected void updateText(final Presentation presentation, final String actionText) {
      presentation.setText(actionText.length() > 0 ? ExecutionBundle.message("create.run.configuration.for.item.action.name", actionText) + "..."
                                                   : ExecutionBundle.message("create.run.configuration.action.name"), false);
    }

    @Override
    public void perform(final ConfigurationContext context) {
      final RunnerAndConfigurationSettings configuration = context.getConfiguration();
      if (ApplicationManager.getApplication().isUnitTestMode() ||
          RunDialog.editConfiguration(context.getProject(), configuration,
                                      ExecutionBundle.message("create.run.configuration.for.item.dialog.title", configuration.getName()))) {
        final RunManagerImpl runManager = (RunManagerImpl)context.getRunManager();
        runManager.addConfiguration(configuration);
        runManager.setSelectedConfiguration(configuration);
      }
    }
  }

  private static class EditPolicy extends CreateAndEditPolicy {
    @Override
    protected void updateText(final Presentation presentation, final String actionText) {
      presentation.setText(actionText.length() > 0 ? ExecutionBundle.message("edit.run.configuration.for.item.action.name", actionText) + "..."
                                                   : ExecutionBundle.message("edit.run.configuration.action.name"), false);
    }

    @Override
    public void perform(final ConfigurationContext context) {
      final RunnerAndConfigurationSettings configuration = context.getConfiguration();
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        RunDialog.editConfiguration(context.getProject(), configuration,
                                    ExecutionBundle.message("edit.run.configuration.for.item.dialog.title", configuration.getName()));
      }
    }
  }

  private static final BaseCreatePolicy CREATE_AND_EDIT = new CreateAndEditPolicy();
  private static final BaseCreatePolicy EDIT = new EditPolicy();
}
