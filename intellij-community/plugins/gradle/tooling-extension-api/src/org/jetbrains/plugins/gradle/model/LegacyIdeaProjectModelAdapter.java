// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.tooling.model.BuildIdentifier;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.ProjectIdentifier;
import org.gradle.tooling.model.ProjectModel;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public class LegacyIdeaProjectModelAdapter implements Build {
  private final BuildIdentifier myBuildIdentifier;
  private final Collection<ProjectModel> myProjectModels;

  public LegacyIdeaProjectModelAdapter(@NotNull IdeaProject ideaProject) {
    DomainObjectSet<? extends IdeaModule> ideaModules = ideaProject.getChildren();
    assert !ideaModules.isEmpty();
    IdeaModule ideaModule = ideaModules.getAt(0);
    myBuildIdentifier = ideaModule.getGradleProject().getProjectIdentifier().getBuildIdentifier();
    myProjectModels = new ArrayList<ProjectModel>(ideaModules.size());
    for (final IdeaModule module : ideaModules) {
      myProjectModels.add(new ProjectModel() {
        @Override
        public ProjectIdentifier getProjectIdentifier() {
          return module.getGradleProject().getProjectIdentifier();
        }
      });
    }
  }

  @Override
  public BuildIdentifier getBuildIdentifier() {
    return myBuildIdentifier;
  }

  @Override
  public Collection<ProjectModel> getProjects() {
    return myProjectModels;
  }
}
