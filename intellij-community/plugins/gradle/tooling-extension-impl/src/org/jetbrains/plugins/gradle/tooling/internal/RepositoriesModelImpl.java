// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.internal;

import org.jetbrains.plugins.gradle.model.MavenRepositoryModel;
import org.jetbrains.plugins.gradle.model.RepositoriesModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class RepositoriesModelImpl implements RepositoriesModel {
  private final Set<MavenRepositoryModel> myRepositories = new HashSet<MavenRepositoryModel>();

  @Override
  public void add(MavenRepositoryModel model) {
    myRepositories.add(model);
  }

  @Override
  public Collection<MavenRepositoryModel> getAll() {
    return new HashSet<MavenRepositoryModel>(myRepositories);
  }
}
