// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Component
import java.awt.Graphics2D
import javax.swing.Icon

/**
 * Draws image. If you need to position image inside inlay, use [InsetPresentation]
 */
class IconPresentation(var icon: Icon, var component: Component) : BasePresentation() {
  override val width: Int
    get() = icon.iconWidth
  override val height: Int
    get() = icon.iconHeight

  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    icon.paintIcon(component, g, 0, 0)
  }

  override fun toString(): String = "<image>"
}