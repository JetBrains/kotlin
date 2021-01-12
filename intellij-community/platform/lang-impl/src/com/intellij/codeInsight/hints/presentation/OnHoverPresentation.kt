// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hints.InlayPresentationFactory
import java.awt.Point
import java.awt.event.MouseEvent

/**
 * Pure presentation. If you need to preserve state between updates you should use [StatefulPresentation]
 */
class OnHoverPresentation(
  presentation: InlayPresentation,
  private val onHoverListener: InlayPresentationFactory.HoverListener) : StaticDelegatePresentation(presentation) {

  override fun mouseMoved(event: MouseEvent, translated: Point) {
    super.mouseMoved(event, translated)
    onHoverListener.onHover(event, translated)
  }

  override fun mouseExited() {
    super.mouseExited()
    onHoverListener.onHoverFinished()
  }
}