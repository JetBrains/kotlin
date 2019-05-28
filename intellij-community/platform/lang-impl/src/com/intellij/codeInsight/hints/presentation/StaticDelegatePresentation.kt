// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent

abstract class StaticDelegatePresentation(val presentation: InlayPresentation) : InlayPresentation {
  // Note: we can't use just delegation using "by", because we need special handling of updateState

  override val width: Int
    get() = presentation.width
  override val height: Int
    get() = presentation.height

  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    presentation.paint(g, attributes)
  }

  override fun fireSizeChanged(previous: Dimension, current: Dimension) {
    presentation.fireSizeChanged(previous, current)
  }

  override fun fireContentChanged(area: Rectangle) {
    presentation.fireContentChanged(area)
  }

  override fun addListener(listener: PresentationListener) {
    presentation.addListener(listener)
  }

  override fun removeListener(listener: PresentationListener) {
    presentation.removeListener(listener)
  }

  override fun updateState(previousPresentation: InlayPresentation) : Boolean {
    if (previousPresentation !is StaticDelegatePresentation) {
      return true
    }
    return presentation.updateState(previousPresentation.presentation)
  }

  override fun toString(): String {
    return presentation.toString()
  }

  override fun mouseClicked(event: MouseEvent, translated: Point) {
    presentation.mouseClicked(event, translated)
  }

  override fun mouseMoved(event: MouseEvent, translated: Point) {
    presentation.mouseMoved(event, translated)
  }

  override fun mouseExited() {
    presentation.mouseExited()
  }
}