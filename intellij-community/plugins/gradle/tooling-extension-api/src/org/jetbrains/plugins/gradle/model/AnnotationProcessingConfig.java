// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface AnnotationProcessingConfig {
  /**
   * Annotation processor path.
   *
   * Contains annotation processor along with all transitive dependencies
   * @return see above
   */
  @NotNull Collection<String> getAnnotationProcessorPath();

  /**
   * Annotation processor arguments
   * @return see above
   */
  @NotNull Collection<String> getAnnotationProcessorArguments();

}
