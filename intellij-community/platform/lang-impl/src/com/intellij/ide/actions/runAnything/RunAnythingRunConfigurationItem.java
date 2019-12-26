// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.execution.actions.ChooseRunConfigurationPopup;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.ide.actions.runAnything.items.RunAnythingItemBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class RunAnythingRunConfigurationItem extends RunAnythingItemBase {
  private final ChooseRunConfigurationPopup.ItemWrapper myWrapper;

  public RunAnythingRunConfigurationItem(@NotNull ChooseRunConfigurationPopup.ItemWrapper wrapper, @Nullable Icon icon) {
    super(wrapper.getText(), icon);
    myWrapper = wrapper;
  }

  @Nullable
  @Override
  public String getDescription() {
    ConfigurationType type = myWrapper.getType();
    return type == null ? null : type.getConfigurationTypeDescription();
  }
}