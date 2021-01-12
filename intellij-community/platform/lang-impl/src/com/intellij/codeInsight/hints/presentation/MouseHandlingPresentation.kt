// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hints.InlayPresentationFactory
import org.jetbrains.annotations.ApiStatus
import java.awt.Point
import java.awt.event.MouseEvent

/**
 * Presentation that allow to setup reaction to mouse actions.
 */
@ApiStatus.Experimental
class MouseHandlingPresentation(
  presentation: InlayPresentation,
  private val clickListener: InlayPresentationFactory.ClickListener?,
  private val hoverListener: InlayPresentationFactory.HoverListener?
) : StaticDelegatePresentation(presentation) {
  override fun mouseClicked(event: MouseEvent, translated: Point) {
    super.mouseClicked(event, translated)
    clickListener?.onClick(event, translated)
  }

  override fun mouseMoved(event: MouseEvent, translated: Point) {
    super.mouseMoved(event, translated)
    hoverListener?.onHover(event, translated)
  }

  override fun mouseExited() {
    super.mouseExited()
    hoverListener?.onHoverFinished()
  }
}