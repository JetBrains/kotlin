package com.intellij.util.indexing.diagnostic.dump

import com.intellij.util.indexing.diagnostic.dump.paths.IndexedFilePath

data class IndexContentDiagnostic(
  val allIndexedFilePaths: List<IndexedFilePath>,
  /**
   * Keys - IDs that have provided (shared) indexes.
   * Values - IDs of files that have provided indexes.
   */
  val providedIndexIdToFileIds: Map<String, Set<Int>>,
  /**
   * Paths to indexed files from *unsupported* file systems (currently, only local and archive file systems are supported).
   */
  val filesFromUnsupportedFileSystems: List<IndexedFilePath>,
  /**
   * Keys - debug name of indexable file provider that schedules a set of files for indexing.
   * Values - IDs of files that were scheduled for indexing by a provider.
   */
  val projectIndexedFileProviderDebugNameToFileIds: Map<String, Set<Int>>
)