// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.preview

import com.intellij.openapi.Disposable
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

internal class IntentionPreviewEditor(private val psiFileCopy: PsiFile, private var caretOffset: Int) : UserDataHolderBase(), Editor {
  private val document: Document = FileDocumentManager.getInstance().getDocument(psiFileCopy.viewProvider.virtualFile)
                                   ?: throw IllegalStateException("Document should be not null.")

  private val caretModel = IntentionPreviewCaretModel()
  override fun getDocument(): Document = document
  override fun getCaretModel() = caretModel

  override fun isViewer(): Boolean = true
  override fun getComponent(): JComponent = throw IntentionPreviewUnsupportedOperationException()
  override fun getContentComponent(): JComponent = throw IntentionPreviewUnsupportedOperationException()
  override fun setBorder(border: Border?) = throw IntentionPreviewUnsupportedOperationException()
  override fun getInsets(): Insets = throw IntentionPreviewUnsupportedOperationException()
  override fun getMarkupModel(): MarkupModel = throw IntentionPreviewUnsupportedOperationException()
  override fun getScrollingModel(): ScrollingModel = throw IntentionPreviewUnsupportedOperationException()
  override fun getSoftWrapModel(): SoftWrapModel = throw IntentionPreviewUnsupportedOperationException()
  override fun getSettings(): EditorSettings = throw IntentionPreviewUnsupportedOperationException()
  override fun getColorsScheme(): EditorColorsScheme = throw IntentionPreviewUnsupportedOperationException()
  override fun getLineHeight(): Int = throw IntentionPreviewUnsupportedOperationException()
  override fun logicalPositionToXY(pos: LogicalPosition): Point = throw IntentionPreviewUnsupportedOperationException()
  override fun logicalPositionToOffset(pos: LogicalPosition): Int = throw IntentionPreviewUnsupportedOperationException()
  override fun logicalToVisualPosition(logicalPos: LogicalPosition): VisualPosition = throw IntentionPreviewUnsupportedOperationException()
  override fun visualPositionToXY(visible: VisualPosition): Point = throw IntentionPreviewUnsupportedOperationException()
  override fun visualPositionToPoint2D(pos: VisualPosition): Point2D = throw IntentionPreviewUnsupportedOperationException()
  override fun visualToLogicalPosition(visiblePos: VisualPosition): LogicalPosition = throw IntentionPreviewUnsupportedOperationException()
  override fun offsetToLogicalPosition(offset: Int): LogicalPosition = throw IntentionPreviewUnsupportedOperationException()
  override fun offsetToVisualPosition(offset: Int): VisualPosition = throw IntentionPreviewUnsupportedOperationException()
  override fun offsetToVisualPosition(offset: Int, leanForward: Boolean, beforeSoftWrap: Boolean): VisualPosition = throw IntentionPreviewUnsupportedOperationException()
  override fun getFoldingModel(): FoldingModel = throw IntentionPreviewUnsupportedOperationException()
  override fun xyToLogicalPosition(p: Point): LogicalPosition = throw IntentionPreviewUnsupportedOperationException()
  override fun xyToVisualPosition(p: Point): VisualPosition = throw IntentionPreviewUnsupportedOperationException()
  override fun xyToVisualPosition(p: Point2D): VisualPosition = throw IntentionPreviewUnsupportedOperationException()
  override fun addEditorMouseListener(listener: EditorMouseListener) = throw IntentionPreviewUnsupportedOperationException()
  override fun removeEditorMouseListener(listener: EditorMouseListener) = throw IntentionPreviewUnsupportedOperationException()
  override fun addEditorMouseMotionListener(listener: EditorMouseMotionListener) = throw IntentionPreviewUnsupportedOperationException()
  override fun removeEditorMouseMotionListener(listener: EditorMouseMotionListener) = throw IntentionPreviewUnsupportedOperationException()
  override fun isDisposed(): Boolean = throw IntentionPreviewUnsupportedOperationException()
  override fun getProject(): Project? = throw IntentionPreviewUnsupportedOperationException()
  override fun isInsertMode(): Boolean = throw IntentionPreviewUnsupportedOperationException()
  override fun getSelectionModel(): SelectionModel = throw IntentionPreviewUnsupportedOperationException()
  override fun isColumnMode(): Boolean = throw IntentionPreviewUnsupportedOperationException()
  override fun isOneLineMode(): Boolean = throw IntentionPreviewUnsupportedOperationException()
  override fun getGutter(): EditorGutter = throw IntentionPreviewUnsupportedOperationException()
  override fun getMouseEventArea(e: MouseEvent): EditorMouseEventArea? = throw IntentionPreviewUnsupportedOperationException()
  override fun setHeaderComponent(header: JComponent?) = throw IntentionPreviewUnsupportedOperationException()
  override fun hasHeaderComponent(): Boolean = throw IntentionPreviewUnsupportedOperationException()
  override fun getHeaderComponent(): JComponent? = throw IntentionPreviewUnsupportedOperationException()
  override fun getIndentsModel(): IndentsModel = throw IntentionPreviewUnsupportedOperationException()
  override fun getInlayModel(): InlayModel = throw IntentionPreviewUnsupportedOperationException()
  override fun getEditorKind(): EditorKind = throw IntentionPreviewUnsupportedOperationException()

  inner class IntentionPreviewCaretModel : CaretModel {
    override fun moveToOffset(offset: Int) { caretOffset = offset }
    override fun getOffset(): Int = caretOffset

    override fun moveCaretRelatively(columnShift: Int, lineShift: Int, withSelection: Boolean, blockSelection: Boolean, scrollToCaret: Boolean) = throw IntentionPreviewUnsupportedOperationException()
    override fun moveToLogicalPosition(pos: LogicalPosition) = throw IntentionPreviewUnsupportedOperationException()
    override fun moveToVisualPosition(pos: VisualPosition) = throw IntentionPreviewUnsupportedOperationException()
    override fun moveToOffset(offset: Int, locateBeforeSoftWrap: Boolean) = throw IntentionPreviewUnsupportedOperationException()
    override fun isUpToDate(): Boolean = throw IntentionPreviewUnsupportedOperationException()
    override fun getLogicalPosition(): LogicalPosition = throw IntentionPreviewUnsupportedOperationException()
    override fun getVisualPosition(): VisualPosition = throw IntentionPreviewUnsupportedOperationException()
    override fun addCaretListener(listener: CaretListener) = throw IntentionPreviewUnsupportedOperationException()
    override fun removeCaretListener(listener: CaretListener) = throw IntentionPreviewUnsupportedOperationException()
    override fun getVisualLineStart(): Int = throw IntentionPreviewUnsupportedOperationException()
    override fun getVisualLineEnd(): Int = throw IntentionPreviewUnsupportedOperationException()
    override fun getTextAttributes(): TextAttributes = throw IntentionPreviewUnsupportedOperationException()
    override fun supportsMultipleCarets(): Boolean = throw IntentionPreviewUnsupportedOperationException()
    override fun getCurrentCaret(): Caret = throw IntentionPreviewUnsupportedOperationException()
    override fun getPrimaryCaret(): Caret = throw IntentionPreviewUnsupportedOperationException()
    override fun getCaretCount(): Int = throw IntentionPreviewUnsupportedOperationException()
    override fun getAllCarets(): MutableList<Caret> = throw IntentionPreviewUnsupportedOperationException()
    override fun getCaretAt(pos: VisualPosition): Caret? = throw IntentionPreviewUnsupportedOperationException()
    override fun addCaret(pos: VisualPosition): Caret? = throw IntentionPreviewUnsupportedOperationException()
    override fun addCaret(pos: VisualPosition, makePrimary: Boolean): Caret? = throw IntentionPreviewUnsupportedOperationException()
    override fun removeCaret(caret: Caret): Boolean = throw IntentionPreviewUnsupportedOperationException()
    override fun removeSecondaryCarets() = throw IntentionPreviewUnsupportedOperationException()
    override fun setCaretsAndSelections(caretStates: MutableList<out CaretState>) = throw IntentionPreviewUnsupportedOperationException()
    override fun setCaretsAndSelections(caretStates: MutableList<out CaretState>, updateSystemSelection: Boolean) = throw IntentionPreviewUnsupportedOperationException()
    override fun getCaretsAndSelections(): MutableList<CaretState> = throw IntentionPreviewUnsupportedOperationException()
    override fun runForEachCaret(action: CaretAction) = throw IntentionPreviewUnsupportedOperationException()
    override fun runForEachCaret(action: CaretAction, reverseOrder: Boolean) = throw IntentionPreviewUnsupportedOperationException()
    override fun addCaretActionListener(listener: CaretActionListener, disposable: Disposable) = throw IntentionPreviewUnsupportedOperationException()
    override fun runBatchCaretOperation(runnable: Runnable) = throw IntentionPreviewUnsupportedOperationException()
  }
}

class IntentionPreviewUnsupportedOperationException
  : UnsupportedOperationException("It's unexpected to invoke this method on an intention preview calculating.")