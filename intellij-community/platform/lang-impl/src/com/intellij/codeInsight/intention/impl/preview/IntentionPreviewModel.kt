// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.preview

import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.fragments.LineFragment
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LineNumberConverter
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
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
import java.awt.Font

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

    fun createEditors(project: Project, result: IntentionPreviewResult?): List<EditorEx> {
      if (result == null) return emptyList()

      val psiFileCopy: PsiFile? = result.psiFile
      val lines: List<LineFragment> = result.lineFragments

      if (psiFileCopy == null) return emptyList()

      lines.forEach { lineFragment -> reformatRange(project, psiFileCopy, lineFragment) }

      val fileText = psiFileCopy.text
      val origText = result.origFile.text
      val diff = ComparisonManager.getInstance().compareLines(origText, fileText,
                                                              ComparisonPolicy.TRIM_WHITESPACES, DumbProgressIndicator.INSTANCE)
      var diffs = diff.mapNotNull { fragment ->
        var start = getOffset(fileText, fragment.startLine2)
        var end = getOffset(fileText, fragment.endLine2)

        if (start > end) return@mapNotNull null

        var text = fileText.substring(start, end).trimStart('\n').trimEnd('\n').trimIndent()
        val deleted = text.isBlank()
        if (deleted) {
          start = getOffset(origText, fragment.startLine1)
          end = getOffset(origText, fragment.endLine1)
          if (start >= end) return@mapNotNull null
          text = origText.substring(start, end).trimStart('\n').trimEnd('\n').trimIndent()
          if (text.isBlank()) return@mapNotNull null
          return@mapNotNull DiffInfo(text, fragment.startLine1, fragment.endLine1 - fragment.startLine1, true)
        }

        return@mapNotNull DiffInfo(text, fragment.startLine1, fragment.endLine2 - fragment.startLine2, false)
      }
      if (diffs.any { info -> !info.deleted }) {
        // Do not display deleted fragments if anything is added
        diffs = diffs.filter { info -> !info.deleted }
      }
      if (diffs.isNotEmpty()) {
        val last = diffs.last()
        val maxLine = last.startLine + last.length
        return diffs.map { it.createEditor(project, result.origFile.fileType, maxLine) }
      }
      return emptyList()
    }

    private data class DiffInfo(val fileText: String,
                                val startLine: Int,
                                val length: Int,
                                val deleted: Boolean) {
      fun createEditor(project: Project,
                       fileType: FileType,
                       maxLine: Int): EditorEx {
        val editor = createEditor(project, fileType, fileText, startLine, maxLine)
        if (deleted) {
          val colorsScheme = editor.colorsScheme
          val attributes = TextAttributes(null, null, colorsScheme.defaultForeground, EffectType.STRIKEOUT, Font.PLAIN)
          val document = editor.document
          val lineCount = document.lineCount
          for (line in 0 until lineCount) {
            var start = document.getLineStartOffset(line)
            var end = document.getLineEndOffset(line) - 1
            while (start <= end && Character.isWhitespace(fileText[start])) start++
            while (start <= end && Character.isWhitespace(fileText[end])) end--
            if (start <= end) {
              editor.markupModel.addRangeHighlighter(start, end + 1, HighlighterLayer.ERROR + 1, attributes,
                                                     HighlighterTargetArea.EXACT_RANGE)
            }
          }
        }
        return editor
      }
    }

    private fun getOffset(fileText: String, lineNumber: Int): Int {
      return StringUtil.lineColToOffset(fileText, lineNumber, 0).let { pos -> if (pos == -1) fileText.length else pos }
    }

    private fun createEditor(project: Project, fileType: FileType, text: String, lineShift: Int, maxLine: Int): EditorEx {
      val editorFactory = EditorFactory.getInstance()
      val document = editorFactory.createDocument(text)
      val editor = (editorFactory.createEditor(document, project, fileType, false) as EditorEx)
        .apply { setBorder(JBUI.Borders.empty(2, 0, 2, 0)) }

      editor.settings.apply {
        isLineNumbersShown = true
        isCaretRowShown = false
        isLineMarkerAreaShown = false
        isFoldingOutlineShown = false
        additionalColumnsCount = 4
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
        setLineNumberConverter(object : LineNumberConverter {
          override fun convert(editor: Editor, line: Int): Int? = line + lineShift
          override fun getMaxLineNumber(editor: Editor): Int? = maxLine
        })
      }

      return editor
    }

    private fun getEditorBackground(): Color {
      val colorsScheme = EditorColorsManager.getInstance().globalScheme
      return colorsScheme.getColor(EditorColors.CARET_ROW_COLOR) ?: colorsScheme.defaultBackground
    }
  }
}