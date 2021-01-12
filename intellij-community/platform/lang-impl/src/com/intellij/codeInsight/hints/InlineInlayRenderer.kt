// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.SequencePresentation
import com.intellij.openapi.editor.Inlay

class InlineInlayRenderer(
  constrainedPresentations: Collection<ConstrainedPresentation<*, HorizontalConstraints>>
) : LinearOrderInlayRenderer<HorizontalConstraints>(
  constrainedPresentations = constrainedPresentations,
  createPresentation = { constrained ->
    when (constrained.size) {
      1 -> constrained.first().root
      else -> SequencePresentation(constrained.map { it.root })
    }
  },
  comparator = COMPARISON
) {
  override fun isAcceptablePlacement(placement: Inlay.Placement): Boolean {
    return placement == Inlay.Placement.INLINE
  }

  companion object {
    private val COMPARISON: (ConstrainedPresentation<*, HorizontalConstraints>) -> Int = { it.priority }
  }
}

