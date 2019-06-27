// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hints.dimension
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent

class SequencePresentation(val presentations: List<InlayPresentation>) : BasePresentation() {
  init {
    if (presentations.isEmpty()) throw IllegalArgumentException()
    for (presentation in presentations) {
      presentation.addListener(InternalListener(presentation))
    }
  }

  fun calcDimensions() {
    width = presentations.sumBy { it.width }
    height = presentations.maxBy { it.height }!!.height
  }

  override var width: Int = 0
  override var height: Int = 0
  init {
    calcDimensions()
  }

  private var presentationUnderCursor: InlayPresentation? = null

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

  private fun handleMouse(e: MouseEvent, original: Point, action: (InlayPresentation, Point) -> Unit) {
    val x = original.x
    val y = original.y
    if (x < 0 || x >= width || y < 0 || y >= height) return
    var xOffset = 0
    for (presentation in presentations) {
      val presentationWidth = presentation.width
      if (x < xOffset + presentationWidth) {
        val translated = original.translateNew(-xOffset, 0)
        action(presentation, translated)
        return
      }
      xOffset += presentationWidth
    }
  }

  override fun mouseClicked(event: MouseEvent, translated: Point) {
    handleMouse(event, translated) { presentation, point ->
      presentation.mouseClicked(event, point)
    }
  }

  override fun mouseMoved(event: MouseEvent, translated: Point) {
    handleMouse(event, translated) { presentation, point ->
      if (presentation != presentationUnderCursor) {
        presentationUnderCursor?.mouseExited()
        presentationUnderCursor = presentation
      }
      presentation.mouseMoved(event, point)
    }
  }

  override fun mouseExited() {
    try {
      presentationUnderCursor?.mouseExited()
    } finally {
      presentationUnderCursor = null
    }
  }

  override fun updateState(previousPresentation: InlayPresentation) : Boolean {
    if (previousPresentation !is SequencePresentation) return true
    if (previousPresentation.presentations.size != presentations.size) return true
    val previousPresentations = previousPresentation.presentations
    var changed = false
    for ((index, presentation) in presentations.withIndex()) {
      if (presentation.updateState(previousPresentations[index])) {
        changed = true
      }
    }
    return changed
  }

  override fun toString(): String = presentations.joinToString(" ", "[", "]") { "$it" }

  inner class InternalListener(private val currentPresentation: InlayPresentation) : PresentationListener {
    override fun contentChanged(area: Rectangle) {
      area.add(shiftOfCurrent(), 0)
      this@SequencePresentation.fireContentChanged(area)
    }

    override fun sizeChanged(previous: Dimension, current: Dimension) {
      val old = dimension()
      calcDimensions()
      val new = dimension()
      this@SequencePresentation.fireSizeChanged(old, new)
    }

    private fun shiftOfCurrent(): Int {
      var shift = 0
      for (presentation in presentations) {
        if (presentation === currentPresentation) {
          return shift
        }
        shift += presentation.width
      }
      throw IllegalStateException()
    }
  }
}