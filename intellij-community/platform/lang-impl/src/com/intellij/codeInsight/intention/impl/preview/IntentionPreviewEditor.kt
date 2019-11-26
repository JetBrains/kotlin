// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiFile
import java.awt.Insets
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.JComponent
import javax.swing.border.Border

class IntentionPreviewEditor(private val psiFileCopy: PsiFile, private var caretOffset: Int) : UserDataHolderBase(), Editor {
  private val document: Document = runReadAction { FileDocumentManager.getInstance().getDocument(psiFileCopy.viewProvider.virtualFile) }
                                   ?: throw IllegalStateException("Document should be not null.")

  companion object {
    const val UNSUPPORTED_MESSAGE: String = "It's unexpected to invoke this method on an intention preview calculating."
  }

  private val caretModel = IntentionPreviewCaretModel()
  override fun getDocument(): Document = document
  override fun getCaretModel() = caretModel

  override fun isViewer(): Boolean = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getComponent(): JComponent = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getContentComponent(): JComponent = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun setBorder(border: Border?) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getInsets(): Insets = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getMarkupModel(): MarkupModel = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getScrollingModel(): ScrollingModel = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getSoftWrapModel(): SoftWrapModel = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getSettings(): EditorSettings = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getColorsScheme(): EditorColorsScheme = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getLineHeight(): Int = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun logicalPositionToXY(pos: LogicalPosition): Point = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun logicalPositionToOffset(pos: LogicalPosition): Int = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun logicalToVisualPosition(logicalPos: LogicalPosition): VisualPosition = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun visualPositionToXY(visible: VisualPosition): Point = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun visualPositionToPoint2D(pos: VisualPosition): Point2D = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun visualToLogicalPosition(visiblePos: VisualPosition): LogicalPosition = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun offsetToLogicalPosition(offset: Int): LogicalPosition = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun offsetToVisualPosition(offset: Int): VisualPosition = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun offsetToVisualPosition(offset: Int, leanForward: Boolean, beforeSoftWrap: Boolean): VisualPosition = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getFoldingModel(): FoldingModel = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun xyToLogicalPosition(p: Point): LogicalPosition = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun xyToVisualPosition(p: Point): VisualPosition = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun xyToVisualPosition(p: Point2D): VisualPosition = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun addEditorMouseListener(listener: EditorMouseListener) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun removeEditorMouseListener(listener: EditorMouseListener) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun addEditorMouseMotionListener(listener: EditorMouseMotionListener) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun removeEditorMouseMotionListener(listener: EditorMouseMotionListener) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun isDisposed(): Boolean = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getProject(): Project? = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun isInsertMode(): Boolean = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getSelectionModel(): SelectionModel = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun isColumnMode(): Boolean = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun isOneLineMode(): Boolean = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getGutter(): EditorGutter = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getMouseEventArea(e: MouseEvent): EditorMouseEventArea? = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun setHeaderComponent(header: JComponent?) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun hasHeaderComponent(): Boolean = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getHeaderComponent(): JComponent? = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getIndentsModel(): IndentsModel = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getInlayModel(): InlayModel = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  override fun getEditorKind(): EditorKind = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)

  inner class IntentionPreviewCaretModel : CaretModel {
    override fun moveToOffset(offset: Int) { caretOffset = offset }
    override fun getOffset(): Int = caretOffset

    override fun moveCaretRelatively(columnShift: Int, lineShift: Int, withSelection: Boolean, blockSelection: Boolean, scrollToCaret: Boolean) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun moveToLogicalPosition(pos: LogicalPosition) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun moveToVisualPosition(pos: VisualPosition) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun moveToOffset(offset: Int, locateBeforeSoftWrap: Boolean) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun isUpToDate(): Boolean = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun getLogicalPosition(): LogicalPosition = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun getVisualPosition(): VisualPosition = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun addCaretListener(listener: CaretListener) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun removeCaretListener(listener: CaretListener) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun getVisualLineStart(): Int = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun getVisualLineEnd(): Int = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun getTextAttributes(): TextAttributes = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun supportsMultipleCarets(): Boolean = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun getCurrentCaret(): Caret = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun getPrimaryCaret(): Caret = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun getCaretCount(): Int = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun getAllCarets(): MutableList<Caret> = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun getCaretAt(pos: VisualPosition): Caret? = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun addCaret(pos: VisualPosition): Caret? = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun addCaret(pos: VisualPosition, makePrimary: Boolean): Caret? = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun removeCaret(caret: Caret): Boolean = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun removeSecondaryCarets() = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun setCaretsAndSelections(caretStates: MutableList<out CaretState>) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun setCaretsAndSelections(caretStates: MutableList<out CaretState>, updateSystemSelection: Boolean) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun getCaretsAndSelections(): MutableList<CaretState> = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun runForEachCaret(action: CaretAction) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun runForEachCaret(action: CaretAction, reverseOrder: Boolean) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun addCaretActionListener(listener: CaretActionListener, disposable: Disposable) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
    override fun runBatchCaretOperation(runnable: Runnable) = throw UnsupportedOperationException(UNSUPPORTED_MESSAGE)
  }
}