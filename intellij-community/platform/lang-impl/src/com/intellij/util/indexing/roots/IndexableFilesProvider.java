// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.util.containers.ConcurrentBitSet;
import org.jetbrains.annotations.ApiStatus;

/**
 * Provides files to be indexed for a project structure entity (module, library, SDK, etc.)
 * Allows the indexing infrastructure to prioritize indexing by some predicate.
 *
 * @see [ModuleIndexableFilesProvider]
 * @see [LibraryIndexableFilesProvider]
 */
@ApiStatus.Internal
public interface IndexableFilesProvider {

  /**
   * Presentable text shown in progress indicator during indexing of files of this provider.
   */
  String getIndexingProgressText();

  /**
   * Presentable text shown in progress indicator during traversing of files of this provider.
   */
  String getRootsScanningProgressText();

  /**
   * Iterates through all files and directories corresponding to this provider.
   * Skips already visited files by looking to [visitedFileSet].
   * @return `false` if [fileIterator] has stopped iteration by returning `false`, `true` otherwise.
   */
  boolean iterateFiles(Project project, ContentIterator fileIterator, ConcurrentBitSet visitedFileSet);
}
