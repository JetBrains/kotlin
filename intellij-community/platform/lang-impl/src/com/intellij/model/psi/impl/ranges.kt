// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.model.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.util.SmartList
import com.intellij.util.containers.minimalElements
import java.util.function.Function

/**
 * @return collection of items with the same minimal range
 * @throws RangeOverlapException if some range overlaps another range
 */
internal fun <X> chooseByRange(items: List<X>, offset: Int, itemRange: (X) -> TextRange): Collection<X> {
  if (items.size < 2) {
    return items
  }
  val toTheLeftOfOffset = SmartList<X>()
  val containingOffset = SmartList<X>()
  for (item in items) {
    if (itemRange(item).endOffset == offset) {
      toTheLeftOfOffset.add(item)
    }
    else {
      containingOffset.add(item)
    }
  }

  if (containingOffset.isNotEmpty()) {
    return containingOffset.minimalElements(Comparator.comparing(Function(itemRange), RANGE_CONTAINS_COMPARATOR))
  }
  else {
    return toTheLeftOfOffset.minimalElements(Comparator.comparing(Function(itemRange), RANGE_START_COMPARATOR_INVERTED))
  }
}

private val RANGE_CONTAINS_COMPARATOR: Comparator<TextRange> = Comparator { range1: TextRange, range2: TextRange ->
  val contains1 = range1.contains(range2)
  val contains2 = range2.contains(range1)
  when {
    contains1 && contains2 -> 0
    contains1 && !contains2 -> 1
    !contains1 && contains2 -> -1
    else -> throw RangeOverlapException(range1, range2)
  }
}

private val RANGE_START_COMPARATOR_INVERTED: Comparator<TextRange> = Comparator { range1: TextRange, range2: TextRange ->
  range2.startOffset - range1.startOffset  // prefer range with greater start offset
}

private class RangeOverlapException(
  range1: TextRange,
  range2: TextRange
) : IllegalArgumentException("Overlapping ranges: $range1 and $range2")
