// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.tooling.util.resolve.deprecated;

import org.jetbrains.plugins.gradle.model.ExternalDependency;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * @deprecated use org.jetbrains.plugins.gradle.tooling.util.resolve.DependencyResolverImpl
 */
@Deprecated
public class ExternalDepsResolutionResult {
  public static final ExternalDepsResolutionResult
    EMPTY = new ExternalDepsResolutionResult(Collections.<ExternalDependency>emptySet(), Collections.<File>emptySet());
  private final Collection<ExternalDependency> externalDeps;
  private final Collection<File> resolvedFiles;

  public ExternalDepsResolutionResult(Collection<ExternalDependency> deps, Collection<File> files) {
    externalDeps = deps;
    resolvedFiles = files;
  }

  public Collection<File> getResolvedFiles() {
    return resolvedFiles;
  }

  public Collection<ExternalDependency> getExternalDeps() {
    return externalDeps;
  }
}
