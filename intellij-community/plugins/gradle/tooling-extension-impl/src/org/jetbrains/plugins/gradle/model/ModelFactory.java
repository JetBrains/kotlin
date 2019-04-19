/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
