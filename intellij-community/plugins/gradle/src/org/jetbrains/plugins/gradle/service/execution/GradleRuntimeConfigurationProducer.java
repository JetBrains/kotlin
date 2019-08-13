// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.execution;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemRunConfigurationProducer;
import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 */
final class GradleRuntimeConfigurationProducer extends AbstractExternalSystemRunConfigurationProducer {
  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return GradleExternalTaskConfigurationType.getInstance().getFactory();
  }
}
