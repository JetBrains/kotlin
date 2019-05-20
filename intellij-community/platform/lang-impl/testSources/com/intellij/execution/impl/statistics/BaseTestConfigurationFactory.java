// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl.statistics;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public abstract class BaseTestConfigurationFactory extends ConfigurationFactory {

  public BaseTestConfigurationFactory(@NotNull ConfigurationType type) {
    super(type);
  }

  protected static class FirstBaseTestConfigurationFactory extends BaseTestConfigurationFactory {
    public static final FirstBaseTestConfigurationFactory INSTANCE = new FirstBaseTestConfigurationFactory();

    public FirstBaseTestConfigurationFactory() {
      super(new BaseTestConfigurationType.FirstTestRunConfigurationType());
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new BaseTestRunConfiguration(project, this) {
      };
    }
  }

  protected static class SecondBaseTestConfigurationFactory extends BaseTestConfigurationFactory {
    public static final SecondBaseTestConfigurationFactory INSTANCE = new SecondBaseTestConfigurationFactory();

    public SecondBaseTestConfigurationFactory() {
      super(new BaseTestConfigurationType.SecondTestRunConfigurationType());
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new BaseTestRunConfiguration(project, this) {
      };
    }
  }

  protected static class MultiFactoryLocalTestConfigurationFactory extends BaseTestConfigurationFactory {
    public static final MultiFactoryLocalTestConfigurationFactory INSTANCE = new MultiFactoryLocalTestConfigurationFactory();

    public MultiFactoryLocalTestConfigurationFactory() {
      super(new BaseTestConfigurationType.MultiFactoryTestRunConfigurationType());
    }

    @NotNull
    @Override
    public String getName() {
      return "Local";
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new BaseTestRunConfiguration(project, this) {
      };
    }
  }

  protected static class MultiFactoryRemoteTestConfigurationFactory extends BaseTestConfigurationFactory {
    public static final MultiFactoryRemoteTestConfigurationFactory INSTANCE = new MultiFactoryRemoteTestConfigurationFactory();

    public MultiFactoryRemoteTestConfigurationFactory() {
      super(new BaseTestConfigurationType.MultiFactoryTestRunConfigurationType());
    }

    @NotNull
    @Override
    public String getName() {
      return "Remote";
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new BaseTestRunConfiguration(project, this) {
      };
    }
  }
}
