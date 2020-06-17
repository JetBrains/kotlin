// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.colors.EditorColorsUtil.getGlobalOrDefaultColorScheme
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import javax.swing.BorderFactory.createEmptyBorder
import javax.swing.JComponent
import javax.swing.JLabel

internal class ProblemsViewPreview(private val panel: ProblemsViewPanel)
  : JLabel(ProblemsViewBundle.message("problems.view.panel.preview.nothing"), CENTER) {

  private var preview: Editor? = null
    set(value) {
      field?.let { EditorFactory.getInstance().releaseEditor(it) }
      field = value
    }

  private fun update(editor: Editor?, component: JComponent?): Editor? {
    panel.secondComponent = component
    preview = editor
    return editor
  }

  fun preview(descriptor: OpenFileDescriptor?, show: Boolean): Editor? {
    if (!show) return update(null, null)
    val file = descriptor?.file ?: return update(null, null)
    val document = ProblemsView.getDocument(panel.project, file) ?: return update(null, null)
    if (preview?.document === document) return preview // nothing is changed

    val editor = EditorFactory.getInstance().createEditor(document, panel.project, EditorKind.PREVIEW)
    if (editor is EditorEx) {
      val scheme = getGlobalOrDefaultColorScheme()
      editor.colorsScheme = scheme
      editor.highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(file, scheme, panel.project)
    }
    with(editor.settings) {
      isAnimatedScrolling = false
      isRefrainFromScrolling = false
      isLineNumbersShown = true
      isFoldingOutlineShown = false
    }
    editor.setBorder(createEmptyBorder())
    return update(editor, editor.component) // show editor preview
  }

  fun findEditor(psi: PsiFile): Editor? {
    return preview ?: PsiDocumentManager.getInstance(psi.project).getDocument(psi)?.let {
      EditorFactory.getInstance().editors(it, psi.project).findFirst().orElse(null)
    }
  }
}
