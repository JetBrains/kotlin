// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.execution.actions;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.RunDialog;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.openapi.actionSystem.Presentation;
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
    if (configuration == null) return CREATE_AND_EDIT;
    final RunManager runManager = context.getRunManager();
    if (runManager.getSelectedConfiguration() != configuration) return SELECT;
    if (configuration.isTemporary()) return SAVE;
    return SELECTED_STABLE;
  }



  private static abstract class BaseCreatePolicy {

    public enum ActionType {
      CREATE, SAVE, SELECT
    }

    private final ActionType myType;

    BaseCreatePolicy(final ActionType type) {
      myType = type;
    }

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

    protected void updateText(final Presentation presentation, final String actionText) {
      presentation.setText(generateName(actionText), false);
    }

    private String generateName(final String actionText) {
      switch(myType) {
        case CREATE: return ExecutionBundle.message("create.run.configuration.for.item.action.name", actionText);
        case SELECT: return ExecutionBundle.message("select.run.configuration.for.item.action.name", actionText);
        default:  return ExecutionBundle.message("save.run.configuration.for.item.action.name", actionText);
      }
    }

    public abstract void perform(ConfigurationContext context);
  }

  private static class SelectPolicy extends BaseCreatePolicy {
    SelectPolicy() {
      super(ActionType.SELECT);
    }

    @Override
    public void perform(final ConfigurationContext context) {
      final RunnerAndConfigurationSettings configuration = context.findExisting();
      if (configuration == null) return;
      context.getRunManager().setSelectedConfiguration(configuration);
    }

    @Override
    protected void updateIcon(final Presentation presentation, final ConfigurationContext context) {
      final RunnerAndConfigurationSettings configuration = context.findExisting();
      if (configuration != null) {
        presentation.setIcon(configuration.getType().getIcon());
      } else {
        super.updateIcon(presentation, context);
      }
    }
  }

  private static class CreatePolicy extends BaseCreatePolicy {
    CreatePolicy() {
      super(ActionType.CREATE);
    }

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
      if (RunDialog.editConfiguration(context.getProject(), configuration, ExecutionBundle.message("create.run.configuration.for.item.dialog.title", configuration.getName()))) {
        final RunManagerImpl runManager = (RunManagerImpl)context.getRunManager();
        runManager.addConfiguration(configuration);
        runManager.setSelectedConfiguration(configuration);
      }
    }
  }

  private static class SavePolicy extends BaseCreatePolicy {
    SavePolicy() {
      super(ActionType.SAVE);
    }

    @Override
    public void perform(final ConfigurationContext context) {
      RunnerAndConfigurationSettings settings = context.findExisting();
      if (settings != null) context.getRunManager().makeStable(settings);
    }

    @Override
    protected void updateIcon(final Presentation presentation, final ConfigurationContext context) {
      final RunnerAndConfigurationSettings configuration = context.findExisting();
      if (configuration != null) {
        presentation.setIcon(configuration.getType().getIcon());
      } else {
        super.updateIcon(presentation, context);
      }
    }
  }

  private static final BaseCreatePolicy CREATE_AND_EDIT = new CreateAndEditPolicy();
  private static final BaseCreatePolicy SELECT = new SelectPolicy();
  private static final BaseCreatePolicy SAVE = new SavePolicy();
  private static final BaseCreatePolicy SELECTED_STABLE = new BaseCreatePolicy(BaseCreatePolicy.ActionType.SELECT) {
    @Override
    public void perform(final ConfigurationContext context) {}

    @Override
    public void update(final Presentation presentation, final ConfigurationContext context, @NotNull final String actionText) {
      super.update(presentation, context, actionText);
      presentation.setVisible(false);
    }
  };
}
