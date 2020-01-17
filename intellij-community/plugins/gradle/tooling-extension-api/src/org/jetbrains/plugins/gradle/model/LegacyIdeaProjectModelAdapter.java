// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.gradle.tooling.model.BuildIdentifier;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.ProjectIdentifier;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public class LegacyIdeaProjectModelAdapter implements Build {
  private final String myName;
  private final BuildIdentifier myBuildIdentifier;
  private final Collection<Project> myProjectModels;

  public LegacyIdeaProjectModelAdapter(@NotNull IdeaProject ideaProject) {
    myName = ideaProject.getName();
    DomainObjectSet<? extends IdeaModule> ideaModules = ideaProject.getChildren();
    assert !ideaModules.isEmpty();
    IdeaModule ideaModule = ideaModules.getAt(0);
    myBuildIdentifier = ideaModule.getGradleProject().getProjectIdentifier().getBuildIdentifier();
    myProjectModels = new ArrayList<Project>(ideaModules.size());
    for (final IdeaModule module : ideaModules) {
      myProjectModels.add(new Project() {
        @Override
        public String getName() {
          return module.getGradleProject().getName();
        }

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
  public String getName() {
    return myName;
  }

  @Override
  public Collection<Project> getProjects() {
    return myProjectModels;
  }
}
