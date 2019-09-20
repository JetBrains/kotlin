// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Map;

/**
 * Model that describes annotation processing settings for a Gradle project
 */
public interface AnnotationProcessingModel extends Serializable {

  /**
   * Get map of source set names to annotation processing configurations
   * @return see above
   */
  @NotNull
  Map<String, AnnotationProcessingConfig> allConfigs();

  /**
   * Get annotation processing configuration for a source set by name
   * @param sourceSetName name of the source set
   * @return configuration or null if source set not found or no annotation processing configured
   */
  @Nullable
  AnnotationProcessingConfig bySourceSetName(@NotNull String sourceSetName);
}
