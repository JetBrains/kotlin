// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataKey;
import org.jetbrains.annotations.NotNull;

interface RunConfigurationCreator {
  DataKey<RunConfigurationCreator> KEY = DataKey.create("RunConfigurationCreator");
  SingleConfigurationConfigurable<RunConfiguration> createNewConfiguration(@NotNull ConfigurationFactory factory);
}
