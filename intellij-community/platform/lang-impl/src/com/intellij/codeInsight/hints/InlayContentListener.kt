// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.PresentationListener
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import java.awt.Dimension
import java.awt.Rectangle

class InlayContentListener(private val inlay: Inlay<out EditorCustomElementRenderer>) : PresentationListener {
    // TODO more precise redraw, requires changes in Inlay
    override fun contentChanged(area: Rectangle) {
      assert(inlay.isValid)
      inlay.repaint()
    }

    override fun sizeChanged(previous: Dimension, current: Dimension) {
      assert(inlay.isValid)
      inlay.update()
    }
  }