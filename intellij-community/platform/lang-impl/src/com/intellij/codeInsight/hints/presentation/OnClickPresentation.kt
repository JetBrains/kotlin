// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import java.awt.Point
import java.awt.event.MouseEvent

class OnClickPresentation(
  presentation: InlayPresentation,
  val onClickAction: (MouseEvent, Point) -> Unit
) : StaticDelegatePresentation(presentation) {
  override fun mouseClicked(e: MouseEvent, editorPoint: Point) {
    super.mouseClicked(e, editorPoint)
    onClickAction(e, editorPoint)
  }
}