// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.Model;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ClassSetBuildImportModelProvider implements ProjectImportModelProvider {
  @NotNull private final Set<Class<?>> classSet;

  public ClassSetBuildImportModelProvider(@NotNull Collection<Class<?>> classes) {
    classSet = new LinkedHashSet<Class<?>>(classes);
  }

  @Override
  public void populateBuildModels(@NotNull BuildController controller,
                                  @NotNull GradleBuild buildModel,
                                  @NotNull BuildModelConsumer consumer) {
    for (Class<?> aClass : classSet) {
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
    // Do nothing, this provider only works on the build model level
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClassSetBuildImportModelProvider provider = (ClassSetBuildImportModelProvider)o;
    if (!classSet.equals(provider.classSet)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    return classSet.hashCode();
  }
}
