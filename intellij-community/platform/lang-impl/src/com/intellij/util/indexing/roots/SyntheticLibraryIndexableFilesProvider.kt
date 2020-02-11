// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.containers.ConcurrentBitSet
import com.intellij.util.indexing.IndexingBundle

internal class SyntheticLibraryIndexableFilesProvider(
  val syntheticLibrary: SyntheticLibrary,
  val project: Project
) : IndexableFilesProvider {

  override fun getPresentableName() = if (syntheticLibrary is ItemPresentation) {
    IndexingBundle.message("indexable.files.provider.presentable.synthetic.library", StringUtil.toLowerCase(syntheticLibrary.presentableText))
  } else {
    IndexingBundle.message("indexable.files.provider.unnamed.synthetic.library")
  }

  override fun iterateFiles(project: Project, fileIterator: ContentIterator, visitedFileSet: ConcurrentBitSet): Boolean {
    val roots = runReadAction { syntheticLibrary.allRoots }
    return IndexableFilesIterationMethods.iterateNonExcludedRoots(project, roots, fileIterator, visitedFileSet)
  }
}