// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.*
import com.intellij.util.containers.ConcurrentBitSet
import com.intellij.util.indexing.IndexableFilesFilter

internal object IndexableFilesIterationMethods {

  private val followSymlinks
    get() = Registry.`is`("indexer.follows.symlinks")

  fun iterateNonExcludedRoots(
    project: Project,
    roots: Iterable<VirtualFile>,
    contentIterator: ContentIterator,
    visitedFileSet: ConcurrentBitSet
  ): Boolean {
    val projectFileIndex = ProjectFileIndex.getInstance(project)
    val acceptFilter = VirtualFileFilter { file ->
      runReadAction { !projectFileIndex.isExcluded(file) }
    }
    return iterateAllRoots(roots, acceptFilter, contentIterator, visitedFileSet)
  }

  fun iterateNonProjectRoots(
    roots: Iterable<VirtualFile>,
    contentIterator: ContentIterator,
    visitedFileSet: ConcurrentBitSet
  ): Boolean = iterateAllRoots(roots, VirtualFileFilter.ALL, contentIterator, visitedFileSet)

  private fun iterateAllRoots(
    roots: Iterable<VirtualFile>,
    fileFilter: VirtualFileFilter,
    contentIterator: ContentIterator,
    visitedFileSet: ConcurrentBitSet
  ): Boolean {
    val finalFilter = fileFilter
      .and { it is VirtualFileWithId && it.id > 0 && !visitedFileSet.set(it.id) }
      .and { IndexableFilesFilter.EP_NAME.extensionList.let { fs -> fs.isEmpty() || fs.any { e -> e.shouldIndex(it) } } }
    return roots.all { root ->
      val options = if (followSymlinks) emptyArray<VirtualFileVisitor.Option>() else arrayOf(VirtualFileVisitor.NO_FOLLOW_SYMLINKS)
      VfsUtilCore.iterateChildrenRecursively(root, finalFilter, contentIterator, *options)
    }
  }
}