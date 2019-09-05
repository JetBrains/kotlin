// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.builder;

import org.gradle.api.invocation.Gradle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext.DataProvider;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

/**
 * Provides {@link DataProvider}s which can be used by different {@link ModelBuilderService}s
 * to avoid unnecessary calculation of the same thing multiple times.
 *
 * @see ModelBuilderService
 * @see ModelBuilderContext
 * @see DataProvider
 *
 * @author Vladislav.Soroka
 */
public interface ModelBuildersDataProviders {
  /**
   * Provides fast access to the {@link org.gradle.api.Project}'s tasks.
   *
   * @see TasksFactory#getTasks(org.gradle.api.Project)
   */
  DataProvider<TasksFactory> TASKS_PROVIDER = new DataProvider<TasksFactory>() {
    @NotNull
    @Override
    public TasksFactory create(@NotNull Gradle gradle) {
      return new TasksFactory();
    }
  };
}
