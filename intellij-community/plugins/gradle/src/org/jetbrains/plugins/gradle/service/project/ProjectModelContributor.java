// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Experimental extension point, it's intended to be replacement for "populate*, create*" methods of {@link GradleProjectResolverExtension}.
 */
@ApiStatus.Experimental
public interface ProjectModelContributor {

  ExtensionPointName<ProjectModelContributor> EP_NAME = ExtensionPointName.create("org.jetbrains.plugins.gradle.projectModelContributor");

  /**
   * @param projectModelBuilder   provides write/read api to contribute to the Gradle IDE project model
   * @param toolingModelsProvider provides facade to work with tooling models obtained by Gradle model builders
   * @param resolverContext       Gradle project resolver context
   */
  void accept(@NotNull ProjectModelBuilder projectModelBuilder,
              @NotNull ToolingModelsProvider toolingModelsProvider,
              @NotNull ProjectResolverContext resolverContext);
}
