// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.internal;

import org.jetbrains.plugins.gradle.model.MavenRepositoryModel;
import org.jetbrains.plugins.gradle.model.RepositoriesModel;

import java.util.Collection;
import java.util.Collections;
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
    return Collections.unmodifiableSet(myRepositories);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RepositoriesModelImpl model = (RepositoriesModelImpl)o;

    if (!myRepositories.equals(model.myRepositories)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myRepositories.hashCode();
  }
}
