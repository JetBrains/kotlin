// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.util.containers.ConcurrentBitSet
import com.intellij.util.indexing.IndexableSetContributor
import com.intellij.util.indexing.IndexingBundle

internal class IndexableSetContributorFilesProvider(val indexableSetContributor: IndexableSetContributor) : IndexableFilesProvider {

  override fun getPresentableName() = IndexingBundle.message("indexable.files.provider.indexed.set.contributor")

  override fun iterateFiles(
    project: Project,
    fileIterator: ContentIterator,
    visitedFileSet: ConcurrentBitSet
  ): Boolean {
    val allRoots = runReadAction {
      indexableSetContributor.getAdditionalProjectRootsToIndex(project) + indexableSetContributor.additionalRootsToIndex
    }
    return IndexableFilesIterationMethods.iterateNonProjectRoots(allRoots, fileIterator, visitedFileSet)
  }

}