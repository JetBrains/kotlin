// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console

import com.intellij.execution.impl.ConsoleViewUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.richcopy.settings.RichCopySettings
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import java.awt.datatransfer.StringSelection
import kotlin.math.max
import kotlin.math.min

/**
 * Created by Yuli Fiterman on 9/17/2016.
 */
class ConsoleHistoryCopyHandler(val originalHandler: EditorActionHandler) : EditorActionHandler() {

  override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
    if (!RichCopySettings.getInstance().isEnabled) {
      return originalHandler.execute(editor, null, dataContext)
    }
    if (true != editor.getUserData(ConsoleViewUtil.EDITOR_IS_CONSOLE_HISTORY_VIEW)
        || editor.caretModel.allCarets.size != 1) {
      return originalHandler.execute(editor, null, dataContext)
    }
    doCopyWithoutPrompt(editor as EditorEx)
  }

  private fun doCopyWithoutPrompt(editor: EditorEx) {
    val start = editor.selectionModel.selectionStart
    val end = editor.selectionModel.selectionEnd
    val document = editor.document
    val beginLine = document.getLineNumber(start)
    val endLine = document.getLineNumber(end)
    val sb = StringBuilder()
    for (i in beginLine..endLine) {
      var lineStart = document.getLineStartOffset(i)
      val r = Ref.create<Int>()
      editor.markupModel.processRangeHighlightersOverlappingWith(lineStart, lineStart) {
        val length = it.getUserData(PROMPT_LENGTH_MARKER) ?: return@processRangeHighlightersOverlappingWith true
        r.set(length)
        false
      }
      if (!r.isNull) {
        lineStart += r.get()
      }
      val rangeStart = max(lineStart, start)
      val rangeEnd = min(document.getLineEndOffset(i), end)
      if (rangeStart < rangeEnd) {
        sb.append(document.getText(TextRange(rangeStart, rangeEnd)))
        if (rangeEnd < end) {
          sb.append("\n")
        }
      }
    }
    if (sb.isNotEmpty()) {
      CopyPasteManager.getInstance().setContents(StringSelection(sb.toString()))
    }
  }

  companion object {
    @JvmField
    val PROMPT_LENGTH_MARKER: Key<Int?> = Key.create<Int>("PROMPT_LENGTH_MARKER")
  }
}
