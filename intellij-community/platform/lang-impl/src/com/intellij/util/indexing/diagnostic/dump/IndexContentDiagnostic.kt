package com.intellij.util.indexing.diagnostic.dump

import com.intellij.util.indexing.diagnostic.dump.paths.IndexedFilePath

data class IndexContentDiagnostic(
  val allIndexedFilePaths: List<IndexedFilePath>,
  val projectIndexedFileProviderDebugNameToOriginalFileIds: Map<String, Set<Int>>
)