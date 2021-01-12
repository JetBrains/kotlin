// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.util.containers.ConcurrentBitSet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

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
   * Presentable name that can be shown in logs and used for debugging purposes.
   */
  @NonNls
  String getDebugName();

  /**
   * Presentable text shown in progress indicator during indexing of files of this provider.
   */
  @NlsContexts.ProgressText
  String getIndexingProgressText();

  /**
   * Presentable text shown in progress indicator during traversing of files of this provider.
   */
  @NlsContexts.ProgressText
  String getRootsScanningProgressText();

  /**
   * Iterates through all files and directories corresponding to this provider.
   * <br />
   * The {@param visitedFileSet} is used to store positive {@link VirtualFileWithId#getId()} of Virtual Files,
   * the implementation is free to skip other {@link VirtualFile} implementations and
   * non-positive {@link VirtualFileWithId#getId()}.
   * The {@param visitedFileSet} is used to implement filtering to skip already visited files by looking to [visitedFileSet].
   * <br />
   * The {@param fileIterator} should be invoked on every new file (with respect to {@oaram visitedFileSet},
   * should the {@link ContentIterator#processFile(VirtualFile)} returns false, the processing should be
   * stopped and the {@code false} should be returned from the method.
   *
   * @return `false` if [fileIterator] has stopped iteration by returning `false`, `true` otherwise.
   */
  boolean iterateFiles(@NotNull Project project,
                       @NotNull ContentIterator fileIterator,
                       @NotNull ConcurrentBitSet visitedFileSet);
}
