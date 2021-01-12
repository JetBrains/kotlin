// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.util.containers.ConcurrentBitSet
import com.intellij.util.indexing.IndexingBundle
import org.jetbrains.annotations.Nullable

internal class SyntheticLibraryIndexableFilesProvider(
  private val syntheticLibrary: SyntheticLibrary
) : IndexableFilesProvider {

  private fun getName() = (syntheticLibrary as? ItemPresentation)?.presentableText

  override fun getDebugName() = getName().takeUnless { it.isNullOrEmpty() }?.let { "Synthetic library '$it'" }
                                ?: syntheticLibrary.toString()

  override fun getIndexingProgressText(): @Nullable String {
    val name = getName()
    if (!name.isNullOrEmpty()) {
      return IndexingBundle.message("indexable.files.provider.indexing.named.provider", name)
    }
    return IndexingBundle.message("indexable.files.provider.indexing.additional.dependencies")
  }

  override fun getRootsScanningProgressText(): String {
    val name = getName()
    if (!name.isNullOrEmpty()) {
      return IndexingBundle.message("indexable.files.provider.scanning.library.name", name)
    }
    return IndexingBundle.message("indexable.files.provider.scanning.additional.dependencies")
  }

  override fun iterateFiles(project: Project, fileIterator: ContentIterator, visitedFileSet: ConcurrentBitSet): Boolean {
    val roots = runReadAction { syntheticLibrary.allRoots }
    return IndexableFilesIterationMethods.iterateNonExcludedRoots(project, roots, fileIterator, visitedFileSet)
  }
}