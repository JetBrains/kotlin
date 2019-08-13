// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import java.io.Serializable;
import java.util.Collection;

/**
 * Container for Maven repositories of a given project
 */
public interface RepositoriesModel extends Serializable {

  void add(MavenRepositoryModel model);

  Collection<MavenRepositoryModel> getAll();
}
