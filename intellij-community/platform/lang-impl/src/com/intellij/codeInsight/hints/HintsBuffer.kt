// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.openapi.editor.Inlay
import gnu.trove.TIntHashSet
import gnu.trove.TIntObjectHashMap

/**
 * Utility class to accumulate hints. Non thread-safe.
 */
class HintsBuffer {
  val inlineHints = TIntObjectHashMap<MutableList<ConstrainedPresentation<*, HorizontalConstraints>>>()
  val blockBelowHints = TIntObjectHashMap<MutableList<ConstrainedPresentation<*, BlockConstraints>>>()
  val blockAboveHints = TIntObjectHashMap<MutableList<ConstrainedPresentation<*, BlockConstraints>>>()

  fun mergeIntoThis(another: HintsBuffer) {
    inlineHints.mergeIntoThis(another.inlineHints)
    blockBelowHints.mergeIntoThis(another.blockBelowHints)
    blockAboveHints.mergeIntoThis(another.blockAboveHints)
  }

  /**
   * Counts all offsets of given [placement] which are not inside [other]
   */
  fun countDisjointElements(other: TIntHashSet, placement: Inlay.Placement): Int {
    val map = getMap(placement)
    var count = 0
    map.forEachKey {
      if (it !in other) count++
      true
    }
    return count
  }

  fun contains(offset: Int, placement: Inlay.Placement): Boolean {
    return getMap(placement).contains(offset)
  }

  fun remove(offset: Int, placement: Inlay.Placement): MutableList<ConstrainedPresentation<*, *>>? {
    return getMap(placement).remove(offset)
  }

  @Suppress("UNCHECKED_CAST")
  private fun getMap(placement: Inlay.Placement) : TIntObjectHashMap<MutableList<ConstrainedPresentation<*, *>>> {
    return when (placement) {
      Inlay.Placement.INLINE -> inlineHints
      Inlay.Placement.ABOVE_LINE -> blockAboveHints
      Inlay.Placement.BELOW_LINE -> blockBelowHints
      Inlay.Placement.AFTER_LINE_END -> TODO()
    } as TIntObjectHashMap<MutableList<ConstrainedPresentation<*, *>>>
  }
}

fun <V>TIntObjectHashMap<MutableList<V>>.mergeIntoThis(another: TIntObjectHashMap<MutableList<V>>) {
  another.forEachEntry { otherOffset, otherList ->
    val current = this[otherOffset]
    if (current == null) {
      put(otherOffset, otherList)
    } else {
      current.addAll(otherList)
    }
    true
  }
}