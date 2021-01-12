// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.Build;
import org.jetbrains.plugins.gradle.model.Project;

import java.util.List;
import java.util.stream.Stream;

/**
 * Experimental extension point, it's intended to be replacement for "*Models, getExtraProject*" methods of {@link ProjectResolverContext}.
 */
@ApiStatus.Experimental
public interface ToolingModelsProvider {
  /**
   * @return Gradle composite root build
   */
  @NotNull
  Build getRootBuild();

  /**
   * @return builds included into the Gradle composite build except the root build
   */
  @NotNull
  List<Build> getIncludedBuilds();

  /**
   * @return {@link Build} the {@link Project} belongs to
   */
  @NotNull
  Build getBuild(@NotNull Project project);

  /**
   * Get tooling model of the type built for the root {@link Build}
   */
  @Nullable
  <T> T getModel(@NotNull Class<T> modelClazz);

  /**
   * Get "build level" tooling model of the specified type built for the {@link Build}
   */
  @Nullable
  <T> T getBuildModel(@NotNull Build build, @NotNull Class<T> modelClazz);

  /**
   * Get "project level" tooling model of the specified type built for the {@link Project}
   */
  @Nullable
  <T> T getProjectModel(@NotNull Project project, @NotNull Class<T> modelClazz);

  /**
   * @return stream to iterate over all builds including the root composite build and included builds
   */
  Stream<Build> builds();

  /**
   * @return stream to iterate over all projects in the Gradle composite
   */
  Stream<Project> projects();
}
