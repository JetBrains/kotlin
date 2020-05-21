// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory.getInstance
import com.intellij.openapi.editor.EditorKind
import javax.swing.BorderFactory.createEmptyBorder
import javax.swing.JComponent
import javax.swing.JLabel

internal class ProblemsViewPreview(private val panel: ProblemsViewPanel)
  : JLabel(ProblemsViewBundle.message("problems.view.panel.preview.nothing"), CENTER) {

  private var preview: Editor? = null
    set(value) {
      field?.let { getInstance().releaseEditor(it) }
      field = value
    }

  private fun update(editor: Editor?, component: JComponent?): Editor? {
    panel.secondComponent = component
    preview = editor
    return editor
  }

  fun preview(document: Document?, show: Boolean): Editor? {
    if (!show) return update(null, null) // hide preview
    if (document == null) return update(null, this) // show label preview
    if (preview?.document === document) return preview // nothing is changed

    val editor = getInstance().createEditor(document, panel.project, EditorKind.PREVIEW)
    with(editor.settings) {
      isAnimatedScrolling = false
      isRefrainFromScrolling = false
      isLineNumbersShown = true
      isFoldingOutlineShown = false
    }
    editor.setBorder(createEmptyBorder())
    return update(editor, editor.component) // show editor preview
  }
}
