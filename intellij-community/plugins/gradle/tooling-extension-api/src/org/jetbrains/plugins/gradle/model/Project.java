// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.tooling.model.ProjectModel;

/**
 * {@link Project} is a "lightweight" model represents a Gradle project.
 * It can be used to access tooling models built for related Gradle project.
 *
 * This is a replacement for {@link org.gradle.tooling.model.idea.IdeaModule}
 *
 * @see org.jetbrains.plugins.gradle.service.project.ToolingModelsProvider#getProjectModel(Project, Class)
 *
 * @author Vladislav.Soroka
 */
public interface Project extends ProjectModel {
  String getName();
}
