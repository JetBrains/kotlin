// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.IndexableSetContributor

data class IndexableSetContributorRootsProvider(
  val indexableSetContributor: IndexableSetContributor,
  val project: Project
) : IndexableRootsProvider {

  override val presentableName
    get() = "Additional roots"

  override fun getRootsToIndex() = runReadAction {
    val roots = linkedSetOf<VirtualFile>()
    roots += indexableSetContributor.getAdditionalProjectRootsToIndex(project)
    roots += indexableSetContributor.additionalRootsToIndex
    roots
  }

}