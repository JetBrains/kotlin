// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.openapi.externalSystem.model.Key;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Experimental builder of the IDE project model based on Gradle project data resolved by {@link ProjectModelContributor}s.
 * The underlying implementation should be able to build IDE project model e.g. {@link com.intellij.openapi.externalSystem.model.DataNode} graph or
 * new upcoming {@link com.intellij.workspace}
 */
@ApiStatus.Experimental
public interface ProjectModelBuilder extends GradleProjectModel {
  <T> ProjectModelBuilder addProjectData(@NotNull Key<T> key, @NotNull T data);
}
