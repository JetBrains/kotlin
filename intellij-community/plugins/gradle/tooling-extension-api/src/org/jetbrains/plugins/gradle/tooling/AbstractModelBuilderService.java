// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public abstract class AbstractModelBuilderService implements ModelBuilderService {
  @Override
  final public Object buildAll(String modelName, Project project) {
    throw new AssertionError("The method should not be called for this service: " + getClass());
  }

  public abstract Object buildAll(@NotNull String modelName, @NotNull Project project, @NotNull ModelBuilderContext context);
}
