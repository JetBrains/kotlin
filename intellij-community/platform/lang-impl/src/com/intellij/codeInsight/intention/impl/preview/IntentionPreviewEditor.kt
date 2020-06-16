// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.preview

import com.intellij.openapi.editor.impl.EmptySoftWrapModel
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.SoftWrapModel
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.psi.PsiFile
import kotlin.math.min

internal class IntentionPreviewEditor(psiFileCopy: PsiFile, caretOffset: Int)
  : ImaginaryEditor(psiFileCopy.project, psiFileCopy.viewProvider.document!!) {

  init {
    caretModel.moveToOffset(caretOffset)
  }

  override fun notImplemented(): RuntimeException = IntentionPreviewUnsupportedOperationException()

  override fun isViewer(): Boolean = true

  override fun logicalPositionToOffset(pos: LogicalPosition): Int {
    val document = document
    val lineStart = document.getLineStartOffset(pos.line)
    val lineEnd = document.getLineEndOffset(pos.line)
    return min(lineEnd, lineStart + pos.column)
  }

  override fun logicalToVisualPosition(logicalPos: LogicalPosition): VisualPosition {
    // No folding support: logicalPos is always the same as visual pos
    return VisualPosition(logicalPos.line, logicalPos.column)
  }

  override fun visualToLogicalPosition(visiblePos: VisualPosition): LogicalPosition {
    return LogicalPosition(visiblePos.line, visiblePos.column)
  }

  override fun offsetToLogicalPosition(offset: Int): LogicalPosition {
    val document = document
    val line = document.getLineNumber(offset)
    val col = offset - document.getLineStartOffset(line)
    return LogicalPosition(line, col)
  }

  override fun getSoftWrapModel(): SoftWrapModel = EmptySoftWrapModel()
}

class IntentionPreviewUnsupportedOperationException
  : UnsupportedOperationException("It's unexpected to invoke this method on an intention preview calculating.")