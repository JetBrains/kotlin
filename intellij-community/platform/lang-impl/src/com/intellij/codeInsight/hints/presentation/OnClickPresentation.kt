// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import java.awt.Point
import java.awt.event.MouseEvent

/**
 * Pure presentation. If you need to preserve state between updates, you should use [StatefulPresentation] or [ChangeOnClickPresentation]
 */
class OnClickPresentation(
  presentation: InlayPresentation,
  val onClickAction: (MouseEvent, Point) -> Unit
) : StaticDelegatePresentation(presentation) {
  override fun mouseClicked(event: MouseEvent, translated: Point) {
    super.mouseClicked(event, translated)
    onClickAction(event, translated)
  }
}