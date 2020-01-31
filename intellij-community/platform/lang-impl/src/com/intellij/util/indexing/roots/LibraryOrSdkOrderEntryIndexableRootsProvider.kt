// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.roots.LibraryOrSdkOrderEntry
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFile

data class LibraryOrSdkOrderEntryIndexableRootsProvider(val orderEntry: LibraryOrSdkOrderEntry) : IndexableRootsProvider {
  override val presentableName
    get() = "Roots of ${orderEntry.presentableName}"

  override fun getRootsToIndex() = runReadAction {
    val roots = linkedSetOf<VirtualFile>()
    roots += orderEntry.getRootFiles(OrderRootType.SOURCES)
    roots += orderEntry.getRootFiles(OrderRootType.CLASSES)
    roots
  }
}