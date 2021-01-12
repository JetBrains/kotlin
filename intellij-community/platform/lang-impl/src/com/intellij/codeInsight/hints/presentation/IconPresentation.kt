// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hints.fireContentChanged
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.AlphaComposite
import java.awt.Component
import java.awt.Graphics2D
import javax.swing.Icon

/**
 * Draws image. If you need to position image inside inlay, use [InsetPresentation]
 */
class IconPresentation(icon: Icon, private val component: Component) : BasePresentation() {
  var icon = icon
  set(value) {
    field = value
    fireContentChanged()
  }

  override val width: Int
    get() = icon.iconWidth
  override val height: Int
    get() = icon.iconHeight

  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    val graphics = g.create() as Graphics2D
    graphics.composite = AlphaComposite.SrcAtop.derive(1.0f)
    icon.paintIcon(component, graphics, 0, 0)
    graphics.dispose()
  }

  override fun toString(): String = "<image>"
}