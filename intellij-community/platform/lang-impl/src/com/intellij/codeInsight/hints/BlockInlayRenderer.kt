// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.VerticalListInlayPresentation
import com.intellij.openapi.editor.Inlay

class BlockInlayRenderer(
  constrainedPresentations: Collection<ConstrainedPresentation<*, BlockConstraints>>
) : LinearOrderInlayRenderer<BlockConstraints>(
  constrainedPresentations = constrainedPresentations,
  createPresentation = { constrained ->
    when (constrained.size) {
      1 -> constrained.first().root
      else -> VerticalListInlayPresentation(constrained.map { it.root })
    }

  },
  comparator = COMPARISON
) {

  override fun isAcceptablePlacement(placement: Inlay.Placement): Boolean {
    return placement == Inlay.Placement.BELOW_LINE || placement == Inlay.Placement.ABOVE_LINE
  }

  companion object {
    private val COMPARISON: (ConstrainedPresentation<*, BlockConstraints>) -> Int = { it.priority }
  }
}