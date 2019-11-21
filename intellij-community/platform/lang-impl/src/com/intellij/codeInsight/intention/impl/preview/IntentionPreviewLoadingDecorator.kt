// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.preview

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LoadingDecorator
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.AsyncProcessIcon
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel

class IntentionPreviewLoadingDecorator(panel: JPanel, project: Project) :
  LoadingDecorator(panel, project, 500, false, AsyncProcessIcon("IntentionPreviewProcessLoading")) {
  override fun customizeLoadingLayer(parent: JPanel, text: JLabel, icon: AsyncProcessIcon): NonOpaquePanel {
    val opaquePanel =
      NonOpaquePanel(BorderLayout(100, 100)).also {
        it.isOpaque = true
        it.background = ColorUtil.withAlpha(EditorColorsManager.getInstance().globalScheme.defaultBackground, 0.6)
        it.add(icon)
        it.add(text)
      }

    parent.layout = BorderLayout()
    parent.add(opaquePanel)

    val font = EditorColorsManager.getInstance().globalScheme.getFont(EditorFontType.PLAIN)
    text.font = font.deriveFont(font.style, (font.size + 2).toFloat());

    return opaquePanel
  }
}