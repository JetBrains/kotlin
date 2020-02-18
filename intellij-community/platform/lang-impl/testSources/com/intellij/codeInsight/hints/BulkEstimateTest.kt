// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.openapi.editor.Inlay
import junit.framework.TestCase
import java.util.stream.IntStream

class BulkEstimateTest : TestCase() {
  fun testAddToExisting() = checkChanges(
    existing = intArrayOf(2, 4, 5, 6),
    collected = intArrayOf(1, 2, 3, 4, 5, 6),
    expectedChanges = 2
  )

  fun testRemoveExisting() = checkChanges(
    existing = intArrayOf(1, 2, 3, 4, 5, 6),
    collected = intArrayOf(2, 4, 5),
    expectedChanges = 3
  )

  fun testRemoveAndAdd() = checkChanges(
    existing = intArrayOf(1, 2, 3, 4, 5, 6),
    collected = intArrayOf(3, 4, 7, 8),
    expectedChanges = 6
  )

  private fun checkChanges(collected: IntArray, existing: IntArray, expectedChanges: Int) {
    val buffer = HintsBuffer()
    addInline(buffer, *collected)
    val actualChangesCount = InlayHintsPass.estimateChangesCountForPlacement(
      existingInlayOffsets = IntStream.of(*existing),
      collected = buffer,
      placement = Inlay.Placement.INLINE
    )
    assertEquals(expectedChanges, actualChangesCount)
  }

  private fun addInline(buffer: HintsBuffer, vararg offsets: Int) {
    for (offset in offsets) {
      addInline(buffer, offset)
    }
  }

  private fun addInline(buffer: HintsBuffer, offset: Int) {
    buffer.inlineHints.put(offset, mutableListOf(HorizontalConstrainedPresentation(TestRootPresentation(1), null)))
  }
}