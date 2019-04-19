// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;


import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Allows the {@link ProjectImportAction} to be extended to allow extra flexibility to extensions when requesting the models.
 *
 * {@link #populateProjectModels(BuildController, IdeaModule, ProjectModelConsumer)} is called once for each {@link IdeaModule} obtained
 * from the Gradle Tooling API (this includes modules from included builds).
 *
 * {@link #populateBuildModels(BuildController, IdeaProject, BuildModelConsumer)} is called once for each {@link IdeaProject} that is
 * obtained from the Gradle Tooling API, for none-composite builds this will be called exactly once, for composite builds this will be
 * called once for each included build and once for the name build. This will always be called after
 * {@link #populateProjectModels(BuildController, IdeaModule, ProjectModelConsumer)}.
 */
public interface ProjectImportExtraModelProvider extends Serializable {
  interface ProjectModelConsumer {
    void consume(@NotNull Object object, @NotNull Class clazz);
  }

  interface BuildModelConsumer {
    void consume(@Nullable IdeaModule module, @NotNull Object object, @NotNull Class  clazz);
  }

  void populateBuildModels(@NotNull BuildController controller, @NotNull IdeaProject project, @NotNull BuildModelConsumer consumer);

  void populateProjectModels(@NotNull BuildController controller, @Nullable IdeaModule module, @NotNull ProjectModelConsumer modelConsumer);
}
