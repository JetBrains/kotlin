// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.RecursivelyUpdatingRootPresentation
import com.intellij.codeInsight.hints.presentation.RootInlayPresentation
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.util.SmartList
import gnu.trove.TIntObjectHashMap

class InlayHintsSinkImpl(val editor: Editor) : InlayHintsSink {
  private val buffer = HintsBuffer()
  private val document: Document = editor.document

  override fun addInlineElement(offset: Int, relatesToPrecedingText: Boolean, presentation: InlayPresentation) {
    addInlineElement(offset, RecursivelyUpdatingRootPresentation(presentation), HorizontalConstraints(0, relatesToPrecedingText))
  }

  override fun addInlineElement(offset: Int, presentation: RootInlayPresentation<*>, constraints: HorizontalConstraints?) {
    buffer.inlineHints.addCreatingListIfNeeded(offset, HorizontalConstrainedPresentation(presentation, constraints))
  }

  override fun addBlockElement(offset: Int,
                               relatesToPrecedingText: Boolean,
                               showAbove: Boolean,
                               priority: Int,
                               presentation: InlayPresentation) {
    if (editor.isDisposed) return
    val line = document.getLineNumber(offset)
    val root = RecursivelyUpdatingRootPresentation(presentation)
    addBlockElement(line, showAbove, root, BlockConstraints(relatesToPrecedingText, priority)) // TODO here lines are applied
  }

  override fun addBlockElement(logicalLine: Int,
                               showAbove: Boolean,
                               presentation: RootInlayPresentation<*>,
                               constraints: BlockConstraints?) {
    val map = if (showAbove)  buffer.blockAboveHints else buffer.blockBelowHints
    val offset = document.getLineStartOffset(logicalLine)
    map.addCreatingListIfNeeded(offset, BlockConstrainedPresentation(presentation, constraints))
  }

  internal fun complete(): HintsBuffer {
    return buffer
  }

  companion object {
    private fun <T : Any> TIntObjectHashMap<MutableList<ConstrainedPresentation<*, T>>>.addCreatingListIfNeeded(
      offset: Int,
      value: ConstrainedPresentation<*, T>
    ) {
      var list = this[offset]
      if (list == null) {
        list = SmartList()
        put(offset, list)
      }
      list.add(value)
    }
  }
}