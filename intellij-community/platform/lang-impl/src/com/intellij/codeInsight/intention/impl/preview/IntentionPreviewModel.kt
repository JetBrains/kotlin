// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.preview

import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.fragments.LineFragment
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LineNumberConverter
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.util.ui.JBUI
import java.awt.Color

internal class IntentionPreviewModel {
  companion object {
    fun reformatRange(project: Project, psiFileCopy: PsiFile, lineFragment: LineFragment) {
      val start = lineFragment.startOffset2
      val end = lineFragment.endOffset2

      if (start >= end) return

      val document = FileDocumentManager.getInstance().getDocument(psiFileCopy.viewProvider.virtualFile)
      if (document != null) PsiDocumentManager.getInstance(project).commitDocument(document)

      WriteCommandAction.runWriteCommandAction(project, Runnable {
        CodeStyleManager.getInstance(project).reformatRange(psiFileCopy, start, end, true)
      })
    }

    fun createEditors(project: Project, originalFile: PsiFile, result: IntentionPreviewResult?): List<EditorEx> {
      if (result == null) return emptyList()

      val psiFileCopy: PsiFile? = result.psiFile
      val lines: List<LineFragment> = result.lineFragments

      if (psiFileCopy == null) return emptyList()

      lines.forEach { lineFragment -> reformatRange(project, psiFileCopy, lineFragment) }

      return ComparisonManager.getInstance().compareLines(originalFile.text, psiFileCopy.text,
                                                          ComparisonPolicy.TRIM_WHITESPACES, DumbProgressIndicator.INSTANCE)
        .mapNotNull {
          val start = StringUtil.lineColToOffset(psiFileCopy.text, it.startLine2, 0)
          val end = StringUtil.lineColToOffset(psiFileCopy.text, it.endLine2, 0)

          if (start >= end) return@mapNotNull null

          var text = psiFileCopy.text.substring(start, end).trimStart('\n').trimEnd('\n').trimIndent()
          if (text.isBlank()) return@mapNotNull null

          text = text.lines().joinToString(separator = "\n") { line -> "$line    "}

          return@mapNotNull createEditor(project, originalFile.fileType, text, it.startLine1)
        }
    }

    private fun createEditor(project: Project, fileType: FileType, text: String, lineShift: Int): EditorEx {
      val editorFactory = EditorFactory.getInstance()
      val document = editorFactory.createDocument(text)
      val editor = (editorFactory.createEditor(document, project, fileType, false) as EditorEx)
        .apply { setBorder(JBUI.Borders.empty(2, 0, 2, 0)) }

      editor.settings.apply {
        isLineNumbersShown = true
        isCaretRowShown = false
        isLineMarkerAreaShown = false
        isFoldingOutlineShown = false
        additionalColumnsCount = 0
        additionalLinesCount = 0
        isRightMarginShown = false
        isUseSoftWraps = false
        isAdditionalPageAtBottom = false
      }

      editor.backgroundColor = getEditorBackground()

      editor.settings.isUseSoftWraps = true
      editor.scrollingModel.disableAnimation()

      editor.gutterComponentEx.apply {
        setPaintBackground(false)
        setLineNumberConverter(LineNumberConverter.Increasing { _, line -> line + lineShift })
      }

      return editor
    }

    private fun getEditorBackground(): Color {
      val colorsScheme = EditorColorsManager.getInstance().globalScheme
      return colorsScheme.getColor(EditorColors.CARET_ROW_COLOR) ?: colorsScheme.defaultBackground
    }
  }
}