// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class ClassSetProjectImportExtraModelProvider implements ProjectImportExtraModelProvider {
  @NotNull private final Set<Class> classSet = new LinkedHashSet<Class>();

  public ClassSetProjectImportExtraModelProvider(@NotNull Collection<Class> classes) {
    classSet.addAll(classes);
  }

  @Override
  public void populateBuildModels(@NotNull BuildController controller, @NotNull IdeaProject project, @NotNull BuildModelConsumer consumer) {
    // Do nothing, this provider only works on the project model level
  }

  @Override
  public void populateProjectModels(@NotNull BuildController controller,
                                    @Nullable IdeaModule module,
                                    @NotNull ProjectModelConsumer modelConsumer) {
    for (Class<?> aClass : classSet) {
      Object instance = controller.findModel(module, aClass);
      if (instance != null) {
        modelConsumer.consume(instance, aClass);
      }
    }
  }
}
