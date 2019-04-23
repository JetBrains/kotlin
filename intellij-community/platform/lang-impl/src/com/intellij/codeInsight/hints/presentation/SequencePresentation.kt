// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hints.fireContentChanged
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent

/**
 * Allows to chain presentations into sequence. All presentations are aligned to upper border.
 */
class SequencePresentation(private var presentations: List<InlayPresentation>) : BasePresentation() {
  override fun updateIfNecessary(newPresentation: InlayPresentation) : Boolean {
    if (newPresentation !is SequencePresentation) throw IllegalArgumentException()

    if (newPresentation.presentations.size != presentations.size) {
      presentations = newPresentation.presentations
      fireContentChanged()
    }

    val newPresentations = newPresentation.presentations
    for ((index, presentation) in presentations.withIndex()) {
      val new = newPresentations[index]
      if (presentation.updateIfNecessary(new)) {
        fireContentChanged()
        return true
      }
    }
    return false
  }

  init {
    assert(presentations.isNotEmpty())
    for (presentation in presentations) {
      presentation.addListener(object: PresentationListener {
        override fun contentChanged(area: Rectangle) {
          // TODO incorrect area!!!
          this@SequencePresentation.fireContentChanged(area)
        }

        override fun sizeChanged(previous: Dimension, current: Dimension) {
          startOffsets = calculateOffsets()
          // TODO incorrect area!!!
          this@SequencePresentation.fireSizeChanged(previous, current)
        }
      })
    }
  }

  private var presentationUnderCursor: InlayPresentation? = null

  var startOffsets: IntArray = calculateOffsets()

  private fun calculateOffsets(): IntArray {
    var currentOffset = 0
    return IntArray(presentations.size) { index ->
      val oldOffset = currentOffset
      val width = presentations[index].width
      currentOffset = oldOffset + width
      oldOffset
    }
  }

//  TODO cache?
  override val width: Int
    get() = presentations.sumBy { it.width }
  override val height: Int
    get() = presentations.maxBy { it.height }!!.height

  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    var xOffset = 0
    try {
      for (presentation in presentations) {
        presentation.paint(g, attributes)
        xOffset += presentation.width
        g.translate(presentation.width, 0)
      }
    } finally {
      g.translate(-xOffset, 0)
    }
  }

  /**
   * Note: height is not considered
   */
  override fun mouseClicked(e: MouseEvent, editorPoint: Point) {
    handleMouse(e) {
      it.mouseClicked(e, editorPoint)
    }
  }

  private fun handleMouse(e: MouseEvent, action: (InlayPresentation) -> Unit) {
    val x = e.x
    val y = e.y
    if (x < 0 || x >= width || y < 0 || y > height) return
    val index = startOffsets.binarySearch(x)
    val finalIndex = if (index >= 0) {
      index
    } else {
      -index - 2
    }
    if (finalIndex < 0 || index >= presentations.size) return
    val presentation = presentations[finalIndex]
    val offset = startOffsets[finalIndex]
    e.withTranslated(-offset, 0) {
      action(presentation)
    }
  }

  /**
   * Note: height is not considered
   */
  override fun mouseMoved(e: MouseEvent) {
    handleMouse(e) {
      if (it != presentationUnderCursor) {
        presentationUnderCursor?.mouseExited()
        presentationUnderCursor = it
      }
      it.mouseMoved(e)
    }
  }

  override fun mouseExited() {
    try {
      presentationUnderCursor?.mouseExited()
    } finally {
      presentationUnderCursor = null
    }
  }

  override fun toString(): String = presentations.joinToString(" ", "[", "]") { "$it" }
}