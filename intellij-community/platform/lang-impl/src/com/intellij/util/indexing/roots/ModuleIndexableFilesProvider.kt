// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.util.containers.ConcurrentBitSet
import com.intellij.util.indexing.IndexingBundle
import org.jetbrains.annotations.NotNull

internal class ModuleIndexableFilesProvider(val module: Module) : IndexableFilesProvider {
  override fun getDebugName() = "Module '${module.name}'"

  override fun getIndexingProgressText(): @NotNull String {
    if (ModuleType.isInternal(module))
      return IndexingBundle.message("indexable.files.provider.indexing.internal.module.name")
    return IndexingBundle.message("indexable.files.provider.indexing.module.name", module.name)
  }

  override fun getRootsScanningProgressText(): @NotNull String {
    if (ModuleType.isInternal(module))
      return IndexingBundle.message("indexable.files.provider.scanning.internal.module.name")
    return IndexingBundle.message("indexable.files.provider.scanning.module.name", module.name)
  }

  override fun iterateFiles(project: Project, fileIterator: ContentIterator, visitedFileSet: ConcurrentBitSet): Boolean {
    val filter = VirtualFileFilter { file -> file is VirtualFileWithId && file.id > 0 && !visitedFileSet.set(file.id) }
    return ModuleRootManager.getInstance(module).fileIndex.iterateContent(fileIterator, filter)
  }
}