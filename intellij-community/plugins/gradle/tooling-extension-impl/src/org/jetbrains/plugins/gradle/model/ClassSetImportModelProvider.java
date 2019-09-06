// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.Model;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class ClassSetImportModelProvider implements ProjectImportModelProvider {
  @NotNull private final Set<Class> projectModelClasses;
  @NotNull private final Set<Class> buildModelClasses;

  public ClassSetImportModelProvider(@NotNull Set<Class> projectModelClasses, @NotNull Set<Class> buildModelClasses) {
    this.projectModelClasses = projectModelClasses;
    this.buildModelClasses = buildModelClasses;
  }

  @Override
  public void populateBuildModels(@NotNull BuildController controller,
                                  @NotNull GradleBuild buildModel,
                                  @NotNull BuildModelConsumer consumer) {
    for (Class<?> aClass : buildModelClasses) {
      Object instance = controller.findModel(buildModel, aClass);
      if (instance != null) {
        consumer.consume(buildModel, instance, aClass);
      }
    }
  }

  @Override
  public void populateProjectModels(@NotNull BuildController controller,
                                    @Nullable Model module,
                                    @NotNull ProjectModelConsumer modelConsumer) {
    for (Class<?> aClass : projectModelClasses) {
      Object instance = controller.findModel(module, aClass);
      if (instance != null) {
        modelConsumer.consume(instance, aClass);
      }
    }
  }
}
