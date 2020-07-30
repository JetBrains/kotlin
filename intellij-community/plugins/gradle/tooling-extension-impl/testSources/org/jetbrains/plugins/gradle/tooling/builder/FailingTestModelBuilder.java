// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.builder;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

import java.io.Serializable;

public class FailingTestModelBuilder implements ModelBuilderService {
  @Override
  public boolean canBuild(String modelName) {
    return Model.class.getName().equals(modelName);
  }

  @Override
  public Object buildAll(String modelName, Project project) {
    throw new RuntimeException("Boom! '\"{}}\n\t");
  }

  @NotNull
  @Override
  public ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder
      .create(project, e, "Test import errors")
      .withDescription("Unable to import Test model");
  }

  public interface Model extends Serializable {
  }
}
