// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ConcurrentBitSet
import com.intellij.util.indexing.IndexingBundle

class LibraryIndexableFilesProvider(val library: Library) : IndexableFilesProvider {
  override fun getDebugName() = library.name.takeUnless { it.isNullOrEmpty() }?.let { "Library '$it'" } ?: library.toString()

  override fun getIndexingProgressText(): String? {
    val libraryName = library.name
    if (!libraryName.isNullOrEmpty()) {
      return IndexingBundle.message("indexable.files.provider.indexing.library.name", libraryName)
    }
    return IndexingBundle.message("indexable.files.provider.indexing.additional.dependencies")
  }

  override fun getRootsScanningProgressText(): String {
    val libraryName = library.name
    if (!libraryName.isNullOrEmpty()) {
      return IndexingBundle.message("indexable.files.provider.scanning.library.name", libraryName)
    }
    return IndexingBundle.message("indexable.files.provider.scanning.additional.dependencies")
  }

  override fun iterateFiles(project: Project, fileIterator: ContentIterator, visitedFileSet: ConcurrentBitSet): Boolean {
    @Suppress("DuplicatedCode")
    val roots = runReadAction {
      if (Disposer.isDisposed(library)) {
        listOf<VirtualFile>()
      }
      else {
        val rootProvider = library.rootProvider
        rootProvider.getFiles(OrderRootType.SOURCES).toList() + rootProvider.getFiles(OrderRootType.CLASSES)
      }
    }

    return IndexableFilesIterationMethods.iterateNonExcludedRoots(project, roots, fileIterator, visitedFileSet)
  }
}