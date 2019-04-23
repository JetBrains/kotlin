// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hints.dimension
import com.intellij.codeInsight.hints.fireContentChanged
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent

/**
 * Presentation, that delegates to [delegate], which can be dynamically changed.
 */
open class DynamicPresentation(delegate: InlayPresentation) : BasePresentation() {
  private var listener: DelegateListener
  init {
    listener = DelegateListener()
    delegate.addListener(listener)
  }

  var delegate: InlayPresentation = delegate
    set(value) {
      val previousDim = field.dimension()
      val newDim = value.dimension()
      field.removeListener(listener)
      field = value
      listener = DelegateListener()
      value.addListener(listener)
      if (previousDim != newDim) {
        fireSizeChanged(previousDim, newDim)
      }
      fireContentChanged()
    }

  override val width: Int
    get() = delegate.width
  override val height: Int
    get() = delegate.height

  override fun updateIfNecessary(newPresentation: InlayPresentation) : Boolean {
    if (newPresentation !is DynamicPresentation) throw IllegalArgumentException()
    if (delegate.javaClass != newPresentation.delegate) {
      // TODO actually handle situation?
      // TODO keep key inside?
      return true
    }
    return delegate.updateIfNecessary(newPresentation.delegate)
  }

  override fun paint(g: Graphics2D, attributes: TextAttributes) = delegate.paint(g, attributes)

  override fun mouseClicked(e: MouseEvent, editorPoint: Point) = delegate.mouseClicked(e, editorPoint)

  override fun mouseMoved(e: MouseEvent) = delegate.mouseMoved(e)

  override fun mouseExited() = delegate.mouseExited()

  override fun toString(): String = "delegate => $delegate"

  private inner class DelegateListener : PresentationListener {
    override fun contentChanged(area: Rectangle) = fireContentChanged(area)

    override fun sizeChanged(previous: Dimension, current: Dimension) = fireSizeChanged(previous, current)
  }
}