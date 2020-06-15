// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.openapi.editor.Inlay
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntSet

/**
 * Utility class to accumulate hints. Non thread-safe.
 */
class HintsBuffer {
  val inlineHints = Int2ObjectOpenHashMap<MutableList<ConstrainedPresentation<*, HorizontalConstraints>>>()
  internal val blockBelowHints = Int2ObjectOpenHashMap<MutableList<ConstrainedPresentation<*, BlockConstraints>>>()
  internal val blockAboveHints = Int2ObjectOpenHashMap<MutableList<ConstrainedPresentation<*, BlockConstraints>>>()

  internal fun mergeIntoThis(another: HintsBuffer) {
    mergeIntoThis(inlineHints, another.inlineHints)
    mergeIntoThis(blockBelowHints, another.blockBelowHints)
    mergeIntoThis(blockAboveHints, another.blockAboveHints)
  }

  /**
   * Counts all offsets of given [placement] which are not inside [other]
   */
  internal fun countDisjointElements(other: IntSet, placement: Inlay.Placement): Int {
    val map = getMap(placement)
    var count = 0
    val iterator = map.keys.iterator()
    while (iterator.hasNext()) {
      if (!other.contains(iterator.nextInt())) {
        count++
      }
    }
    return count
  }

  internal fun contains(offset: Int, placement: Inlay.Placement): Boolean {
    return getMap(placement).contains(offset)
  }

  fun remove(offset: Int, placement: Inlay.Placement): MutableList<ConstrainedPresentation<*, *>>? {
    return getMap(placement).remove(offset)
  }

  @Suppress("UNCHECKED_CAST")
  private fun getMap(placement: Inlay.Placement) : Int2ObjectMap<MutableList<ConstrainedPresentation<*, *>>> {
    return when (placement) {
      Inlay.Placement.INLINE -> inlineHints
      Inlay.Placement.ABOVE_LINE -> blockAboveHints
      Inlay.Placement.BELOW_LINE -> blockBelowHints
      Inlay.Placement.AFTER_LINE_END -> TODO()
    } as Int2ObjectOpenHashMap<MutableList<ConstrainedPresentation<*, *>>>
  }
}

private fun <V> mergeIntoThis(one: Int2ObjectOpenHashMap<MutableList<V>>, another: Int2ObjectOpenHashMap<MutableList<V>>) {
  val bIterator = another.int2ObjectEntrySet().fastIterator()
  while (bIterator.hasNext()) {
    val otherEntry = bIterator.next()
    val otherOffset = otherEntry.intKey
    val current = one.get(otherOffset)
    if (current == null) {
      one.put(otherOffset, otherEntry.value)
    }
    else {
      current.addAll(otherEntry.value)
    }
  }
}