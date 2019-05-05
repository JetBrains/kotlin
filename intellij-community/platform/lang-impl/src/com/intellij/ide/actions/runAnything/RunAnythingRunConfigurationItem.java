// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything;

import com.intellij.execution.actions.ChooseRunConfigurationPopup;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.ide.actions.runAnything.items.RunAnythingItemBase;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

import static com.intellij.ui.SimpleTextAttributes.STYLE_SMALLER;

public class RunAnythingRunConfigurationItem extends RunAnythingItemBase {
  public static final String RUN_CONFIGURATION_AD_TEXT = RunAnythingUtil.AD_CONTEXT_TEXT + ", " + RunAnythingUtil.AD_DEBUG_TEXT;
  private final ChooseRunConfigurationPopup.ItemWrapper myWrapper;

  public RunAnythingRunConfigurationItem(@NotNull ChooseRunConfigurationPopup.ItemWrapper wrapper, @Nullable Icon icon) {
    super(wrapper.getText(), icon);
    myWrapper = wrapper;
  }

  @NotNull
  @Override
  public Component createComponent(@Nullable String pattern, @Nullable Icon groupIcon, boolean isSelected, boolean hasFocus) {
    JPanel component = (JPanel)super.createComponent(pattern, groupIcon, isSelected, hasFocus);

    ConfigurationType type = myWrapper.getType();
    if (type == null) {
      return component;
    }

    String description = type.getConfigurationTypeDescription();
    if (description == null) {
      return component;
    }

    SimpleColoredComponent descriptionComponent = new SimpleColoredComponent();
    descriptionComponent.append(description, new SimpleTextAttributes(STYLE_SMALLER, UIUtil.getListForeground(isSelected, true)));
    component.add(descriptionComponent, BorderLayout.EAST);

    return component;
  }
}