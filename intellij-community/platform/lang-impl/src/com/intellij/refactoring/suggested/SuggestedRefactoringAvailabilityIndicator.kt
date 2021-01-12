// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.suggested

import com.intellij.codeInsight.daemon.GutterMark
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import org.jetbrains.annotations.TestOnly

class SuggestedRefactoringAvailabilityIndicator(private val project: Project) {
  private class Data(
    val document: Document,
    val highlighterRangeMarker: RangeMarker,
    val availabilityRangeMarker: RangeMarker,
    val refactoringEnabled: Boolean,
    val tooltip: String
  ) {
    override fun equals(other: Any?): Boolean {
      return other is Data
             && other.document == document
             && other.highlighterRangeMarker.range == highlighterRangeMarker.range
             && other.availabilityRangeMarker.range == availabilityRangeMarker.range
             && other.refactoringEnabled == refactoringEnabled
             && other.tooltip == tooltip
    }

    override fun hashCode() = tooltip.hashCode()
  }

  private var data: Data? = null

  private val editorsAndHighlighters = mutableMapOf<Editor, RangeHighlighter?>()

  private val caretListener = object : CaretListener {
    override fun caretPositionChanged(event: CaretEvent) {
      updateHighlighter(event.editor)
    }
  }

  init {
    EditorFactory.getInstance().addEditorFactoryListener(object : EditorFactoryListener {
      override fun editorCreated(event: EditorFactoryEvent) {
        attachToEditor(event.editor)
      }

      override fun editorReleased(event: EditorFactoryEvent) {
        editorsAndHighlighters.remove(event.editor)
      }
    }, project)
  }

  fun show(
    document: Document,
    markerRange: TextRange,
    availabilityRange: TextRange,
    refactoringEnabled: Boolean,
    tooltip: String
  ) {
    ApplicationManager.getApplication().assertIsDispatchThread()

    val newData = Data(
      document,
      document.createRangeMarker(markerRange),
      document.createRangeMarker(availabilityRange).apply { isGreedyToLeft = true; isGreedyToRight = true },
      refactoringEnabled,
      tooltip
    )
    if (newData == data) return

    clear()

    data = newData

    EditorFactory.getInstance().editors(document, project).forEach(::attachToEditor)
  }

  fun clear() {
    ApplicationManager.getApplication().assertIsDispatchThread()
    if (data == null) return

    data = null
    for ((editor, highlighter) in editorsAndHighlighters) {
      editor.caretModel.removeCaretListener(caretListener)
      highlighter?.let { editor.markupModel.removeHighlighter(it) }
    }
    editorsAndHighlighters.clear()
  }

  fun disable() {
    ApplicationManager.getApplication().assertIsDispatchThread()
    val data = data ?: return
    if (data.refactoringEnabled) {
      show(
        data.document,
        data.highlighterRangeMarker.range ?: return,
        data.availabilityRangeMarker.range ?: return,
        false,
        data.tooltip
      )
    }
  }

  @get:TestOnly
  val hasData: Boolean
    get() = data != null

  private fun attachToEditor(editor: Editor) {
    if (editor.document == data?.document) {
      editor.caretModel.addCaretListener(caretListener)
      updateHighlighter(editor)
    }
  }

  private fun updateHighlighter(editor: Editor) {
    ApplicationManager.getApplication().assertIsDispatchThread()

    val prevHighlighter = editorsAndHighlighters[editor]
    if (prevHighlighter != null) {
      editor.markupModel.removeHighlighter(prevHighlighter)
      editorsAndHighlighters.remove(editor)
    }

    val range = data?.availabilityRangeMarker?.range ?: return
    if (!range.containsOffset(editor.caretModel.offset)) return
    val highlighterRange = data!!.highlighterRangeMarker.range ?: return

    val highlighter = editor.markupModel.addRangeHighlighter(
      highlighterRange.startOffset,
      highlighterRange.endOffset,
      HighlighterLayer.LAST,
      TextAttributes(),
      HighlighterTargetArea.EXACT_RANGE
    )
    highlighter.gutterIconRenderer = if (data!!.refactoringEnabled)
      RefactoringAvailableGutterIconRenderer(data!!.tooltip)
    else
      RefactoringDisabledGutterIconRenderer(data!!.tooltip)
    editorsAndHighlighters[editor] = highlighter
  }

  companion object {
    val disabledRefactoringTooltip = RefactoringBundle.message("suggested.refactoring.disabled.gutter.icon.tooltip")
  }
}

class RefactoringAvailableGutterIconRenderer(private val tooltip: String) : GutterIconRenderer() {
  override fun getIcon() = AllIcons.Gutter.SuggestedRefactoringBulb
  override fun getTooltipText() = tooltip
  override fun isNavigateAction() = true
  override fun getAlignment() = Alignment.RIGHT

  override fun getClickAction(): AnAction {
    return object : AnAction() {
      override fun actionPerformed(e: AnActionEvent) {
        val project = e.dataContext.getData(CommonDataKeys.PROJECT)!!
        val editor = e.dataContext.getData(CommonDataKeys.EDITOR)!!

        val gutterComponent = (editor as EditorEx).gutterComponentEx
        val anchor = gutterComponent.getCenterPoint(this@RefactoringAvailableGutterIconRenderer)

        performSuggestedRefactoring(
          project,
          editor,
          gutterComponent,
          anchor,
          showReviewBalloon = true,
          actionPlace = ActionPlaces.EDITOR_GUTTER
        )
      }
    }
  }

  override fun equals(other: Any?) = other === this
  override fun hashCode() = 0
}

class RefactoringDisabledGutterIconRenderer(private val tooltip: String) : GutterIconRenderer() {
  override fun getIcon() = AllIcons.Gutter.SuggestedRefactoringBulbDisabled
  override fun getTooltipText() = tooltip
  override fun getAlignment() = Alignment.RIGHT

  override fun equals(other: Any?) = other === this
  override fun hashCode() = 0
}

internal fun SuggestedRefactoringAvailabilityIndicator.update(
  declaration: PsiElement,
  refactoringData: SuggestedRefactoringData?,
  refactoringSupport: SuggestedRefactoringSupport
) {
  val psiFile = declaration.containingFile
  val psiDocumentManager = PsiDocumentManager.getInstance(psiFile.project)
  val document = psiDocumentManager.getDocument(psiFile)!!
  require(psiDocumentManager.isCommitted(document))

  val refactoringAvailable: Boolean
  val tooltip: String
  val markerRange: TextRange
  val availabilityRange: TextRange?

  when (refactoringData) {
    is SuggestedRenameData -> {
      refactoringAvailable = true
      tooltip = RefactoringBundle.message(
        "suggested.refactoring.rename.gutter.icon.tooltip",
        refactoringData.oldName,
        refactoringData.declaration.name,
        intentionActionShortcutHint()
      )
      markerRange = refactoringSupport.nameRange(refactoringData.declaration)!!
      availabilityRange = markerRange
    }

    is SuggestedChangeSignatureData -> {
      refactoringAvailable = true
      tooltip = RefactoringBundle.message(
        "suggested.refactoring.change.signature.gutter.icon.tooltip",
        refactoringData.nameOfStuffToUpdate,
        refactoringData.oldSignature.name,
        intentionActionShortcutHint()
      )
      markerRange = refactoringSupport.nameRange(refactoringData.declaration)!!
      availabilityRange = refactoringSupport.changeSignatureAvailabilityRange(refactoringData.declaration)
    }

    null -> {
      refactoringAvailable = false
      tooltip = SuggestedRefactoringAvailabilityIndicator.disabledRefactoringTooltip
      markerRange = refactoringSupport.nameRange(declaration)!!
      availabilityRange = refactoringSupport.changeSignatureAvailabilityRange(declaration)
    }
  }

  fun doUpdate() {
    if (availabilityRange != null) {
      show(document, markerRange, availabilityRange, refactoringAvailable, tooltip)
    }
    else {
      clear()
    }
  }

  if (ApplicationManager.getApplication().isDispatchThread) {
    doUpdate()
  }
  else {
    val modificationStamp = document.modificationStamp
    ApplicationManager.getApplication().invokeLater {
      if (document.modificationStamp == modificationStamp) {
        doUpdate()
      }
    }
  }
}

private fun intentionActionShortcutHint(): String {
  val shortcut = ActionManager.getInstance().getKeyboardShortcut("ShowIntentionActions") ?: return ""
  return "(${KeymapUtil.getShortcutText(shortcut)})"
}

internal fun SuggestedRefactoringSupport.changeSignatureAvailabilityRange(declaration: PsiElement): TextRange? {
  val file = declaration.containingFile
  val psiDocumentManager = PsiDocumentManager.getInstance(file.project)
  val document = psiDocumentManager.getDocument(file)!!
  require(psiDocumentManager.isCommitted(document))
  return signatureRange(declaration)
    ?.extend(document.charsSequence) { it == ' ' || it == '\t' }
}

class SuggestedRefactoringGutterMarkPreprocessor : GutterMarkPreprocessor {
  override fun processMarkers(list: List<GutterMark>): List<GutterMark> {
    val mark = list.firstOrNull {
      it is RefactoringAvailableGutterIconRenderer || it is RefactoringDisabledGutterIconRenderer
    } ?: return list
    // in order our gutter icon to be the most right, we must put it the first among ones with Alignment.RIGHT
    return list.toMutableList().apply {
      remove(mark)
      add(0, mark)
    }
  }
}
