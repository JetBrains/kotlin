// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity;

import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ChooseRunConfigurationPopup;
import com.intellij.ide.actions.runAnything.RunAnythingRunConfigurationItem;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.DataContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.ide.actions.runAnything.RunAnythingAction.EXECUTOR_KEY;
import static com.intellij.ide.actions.runAnything.RunAnythingUtil.fetchProject;

public abstract class RunAnythingRunConfigurationProvider extends RunAnythingProviderBase<ChooseRunConfigurationPopup.ItemWrapper> {
  @NotNull
  @Override
  public String getCommand(@NotNull ChooseRunConfigurationPopup.ItemWrapper value) {
    return value.getText();
  }

  @Override
  public void execute(@NotNull DataContext dataContext, @NotNull ChooseRunConfigurationPopup.ItemWrapper wrapper) {
    Executor executor = EXECUTOR_KEY.getData(dataContext);
    assert executor != null;

    Object value = wrapper.getValue();
    if (value instanceof RunnerAndConfigurationSettings &&
        !RunManager.getInstance(fetchProject(dataContext)).hasSettings((RunnerAndConfigurationSettings)value)) {
      RunManager.getInstance(fetchProject(dataContext)).addConfiguration((RunnerAndConfigurationSettings)value);
    }

    wrapper.perform(fetchProject(dataContext), executor, dataContext);
  }

  @Nullable
  @Override
  public Icon getIcon(@NotNull ChooseRunConfigurationPopup.ItemWrapper value) {
    return value.getIcon();
  }

  @Nullable
  @Override
  public String getAdText() {
    return RunAnythingRunConfigurationItem.RUN_CONFIGURATION_AD_TEXT;
  }

  @NotNull
  @Override
  public RunAnythingItem getMainListItem(@NotNull DataContext dataContext, @NotNull ChooseRunConfigurationPopup.ItemWrapper value) {
    return new RunAnythingRunConfigurationItem(value, value.getIcon());
  }
}