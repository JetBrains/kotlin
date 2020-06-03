// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hints.InlayPresentationFactory
import java.awt.Point
import java.awt.event.MouseEvent

/**
 * Pure presentation. If you need to preserve state between updates you should use [StatefulPresentation] or [ChangeOnClickPresentation]
 */
class OnClickPresentation(
  presentation: InlayPresentation,
  private val clickListener: InlayPresentationFactory.ClickListener
) : StaticDelegatePresentation(presentation) {
  constructor(presentation: InlayPresentation, listener: (MouseEvent, Point) -> Unit) : this(
    presentation,
    object : InlayPresentationFactory.ClickListener {
      override fun onClick(event: MouseEvent, translated: Point) {
        listener(event, translated)
      }
    })

  override fun mouseClicked(event: MouseEvent, translated: Point) {
    super.mouseClicked(event, translated)
    clickListener.onClick(event, translated)
  }
}