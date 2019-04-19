// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class FakeConfigurationType implements ConfigurationType {
  @NotNull
  @Override
  public String getDisplayName() {
    return "Fake";
  }

  @Override
  public String getConfigurationTypeDescription() {
    return "Fake";
  }

  @Override
  public Icon getIcon() {
    return null;
  }

  @Override
  @NotNull
  public String getId() {
    return FakeConfigurationType.class.getSimpleName();
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[0];
  }
}
