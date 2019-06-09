// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.model;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public interface ExternalSourceDirectorySet extends Serializable {
  @NotNull
  String getName();

  @NotNull
  Set<File> getSrcDirs();

  @NotNull
  File getOutputDir();

  /**
   * @deprecated use {@link #getGradleOutputDirs()}
   */
  @Deprecated
  @NotNull
  File getGradleOutputDir();

  @NotNull
  Collection<File> getGradleOutputDirs();

  /**
   * Returns {@code true} if compiler output for this ExternalSourceDirectorySet should is inherited from IDEA project
   * @return true if compiler output path is inherited, false otherwise
   */
  boolean isCompilerOutputPathInherited();

  @NotNull
  Set<String> getExcludes();
  @NotNull
  Set<String> getIncludes();

  @NotNull
  FilePatternSet getPatterns();

  @NotNull
  List<? extends ExternalFilter> getFilters();
}
