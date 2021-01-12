// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.builder;

import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.MavenRepositoryModel;
import org.jetbrains.plugins.gradle.model.RepositoriesModel;
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;
import org.jetbrains.plugins.gradle.tooling.internal.MavenRepositoryModelImpl;
import org.jetbrains.plugins.gradle.tooling.internal.RepositoriesModelImpl;

public class MavenRepositoriesModelBuilder implements ModelBuilderService {
  @Override
  public boolean canBuild(String modelName) {
    return RepositoriesModel.class.getName().equals(modelName);
  }

  @Override
  public Object buildAll(String modelName, Project project) {
    final RepositoriesModel repoModel = new RepositoriesModelImpl();
    for (MavenArtifactRepository artifactRepository : project.getRepositories().withType(MavenArtifactRepository.class)) {
      final MavenRepositoryModel model = new MavenRepositoryModelImpl(artifactRepository.getName(), artifactRepository.getUrl().toString());
      repoModel.add(model);
    }
    return repoModel;
  }

  @NotNull
  @Override
  public ErrorMessageBuilder getErrorMessageBuilder(@NotNull Project project, @NotNull Exception e) {
    return ErrorMessageBuilder.create(
      project, e, "Project maven repositories import errors"
    ).withDescription(
      "Unable to obtain information about configured Maven repositories");
  }
}
