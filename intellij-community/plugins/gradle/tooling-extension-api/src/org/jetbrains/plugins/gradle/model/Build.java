// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.tooling.model.BuildModel;

import java.util.Collection;

/**
 * {@link Build} is a "lightweight" model represents a Gradle build.
 * It can be used to access "build-level" tooling models built for related Gradle build.
 *
 * This is a replacement for {@link org.gradle.tooling.model.idea.IdeaProject}
 *
 * @see org.jetbrains.plugins.gradle.service.project.ToolingModelsProvider#getBuildModel(Build, Class)
 *
 * @author Vladislav.Soroka
 */
public interface Build extends BuildModel {
  String getName();

  Collection<Project> getProjects();
}
