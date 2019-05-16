// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public class ModelFactory {
  public static ExternalDependency createCopy(ExternalDependency dependency) {
    ExternalDependency newDep;
    if (dependency instanceof ExternalProjectDependency) {
      newDep = new DefaultExternalProjectDependency((ExternalProjectDependency)dependency);
    }
    else if (dependency instanceof ExternalLibraryDependency) {
      newDep = new DefaultExternalLibraryDependency((ExternalLibraryDependency)dependency);
    }
    else if (dependency instanceof FileCollectionDependency) {
      newDep = new DefaultFileCollectionDependency((FileCollectionDependency)dependency);
    }
    else if (dependency instanceof UnresolvedExternalDependency) {
      newDep = new DefaultUnresolvedExternalDependency((UnresolvedExternalDependency)dependency);
    }
    else {
      throw new AssertionError("unknown dependency object which implements: " + Arrays.toString(dependency.getClass().getInterfaces()));
    }
    return newDep;
  }

  @Contract("null -> null")
  public static Collection<ExternalDependency> createCopy(@Nullable Collection<? extends ExternalDependency> dependencies) {
    if (dependencies == null) return null;

    Collection<ExternalDependency> result = new ArrayList<ExternalDependency>(dependencies.size());
    for (ExternalDependency dependency : dependencies) {
      result.add(createCopy(dependency));
    }
    return result;
  }
}
