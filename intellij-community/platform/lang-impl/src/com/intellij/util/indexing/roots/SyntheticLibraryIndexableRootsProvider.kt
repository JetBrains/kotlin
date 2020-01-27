// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.roots.SyntheticLibrary

data class SyntheticLibraryIndexableRootsProvider(val syntheticLibrary: SyntheticLibrary) : IndexableRootsProvider {
  override val presentableName
    get() = "Roots of " + if (syntheticLibrary is ItemPresentation) syntheticLibrary.presentableText else "synthetic library"

  override fun getRootsToIndex() = runReadAction {
    syntheticLibrary.allRoots.toSet()
  }
}