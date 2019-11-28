// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.preview

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LoadingDecorator
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.components.panels.OpaquePanel
import com.intellij.util.ui.AsyncProcessIcon
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel

internal class IntentionPreviewLoadingDecorator(panel: JPanel, project: Project) :
  LoadingDecorator(panel, project, 500, false, AsyncProcessIcon("IntentionPreviewProcessLoading")) {
  override fun customizeLoadingLayer(parent: JPanel, text: JLabel, icon: AsyncProcessIcon): NonOpaquePanel {
    val editorBackground = EditorColorsManager.getInstance().globalScheme.defaultBackground
    val iconNonOpaquePanel = OpaquePanel(FlowLayout(FlowLayout.RIGHT, 2, 2))
      .apply {
        add(icon, BorderLayout.NORTH)
        background = editorBackground
      }

    icon.background = editorBackground.withAlpha(0.0)
    icon.isOpaque = true

    val opaquePanel = OpaquePanel()
    opaquePanel.background = editorBackground.withAlpha(0.6)

    val nonOpaquePanel = NonOpaquePanel(BorderLayout())
    nonOpaquePanel.add(iconNonOpaquePanel, BorderLayout.EAST)
    nonOpaquePanel.add(opaquePanel, BorderLayout.CENTER)

    parent.layout = BorderLayout()
    parent.add(nonOpaquePanel)

    return nonOpaquePanel
  }

  fun Color.withAlpha(alpha: Double) = ColorUtil.withAlpha(this, alpha)
}