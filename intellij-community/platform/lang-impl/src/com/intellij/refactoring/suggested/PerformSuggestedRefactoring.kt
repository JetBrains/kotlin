// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.suggested

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.LaterInvocator
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.executeCommand
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.suggested.SuggestedRefactoringExecution.NewParameterValue
import com.intellij.refactoring.suggested.SuggestedRefactoringState.ErrorLevel
import com.intellij.refactoring.util.TextOccurrencesUtil
import com.intellij.ui.awt.RelativePoint
import org.jetbrains.annotations.TestOnly
import java.awt.Font
import java.awt.Insets
import java.awt.Point
import java.awt.Toolkit
import javax.swing.JComponent
import javax.swing.UIManager

internal fun performSuggestedRefactoring(
  project: Project,
  editor: Editor,
  popupAnchorComponent: JComponent?,
  popupAnchorPoint: Point?,
  showReviewBalloon: Boolean,
  actionPlace: String
) {
  PsiDocumentManager.getInstance(project).commitAllDocuments()

  val state = SuggestedRefactoringProviderImpl.getInstance(project).state
    ?.takeIf { it.errorLevel == ErrorLevel.NO_ERRORS }
    ?.let {
      it.refactoringSupport.availability.refineSignaturesWithResolve(it)
    } ?: return
  if (state.errorLevel != ErrorLevel.NO_ERRORS || state.oldSignature == state.newSignature) return
  val refactoringSupport = state.refactoringSupport

  when (val refactoringData = refactoringSupport.availability.detectAvailableRefactoring(state)) {
    is SuggestedRenameData -> {
      val popup = RenamePopup(refactoringData.oldName, refactoringData.declaration.name!!)

      fun doRefactor() {
        doRefactor(refactoringData, state, editor, actionPlace) {
          performRename(refactoringSupport, refactoringData, project, editor)
        }
      }

      if (!showReviewBalloon || ApplicationManager.getApplication().isHeadlessEnvironment) {
        doRefactor()
        return
      }

      val rangeToHighlight = state.refactoringSupport.nameRange(state.declaration)!!

      val callbacks = createAndShowBalloon<Unit>(
        popup, project, editor, popupAnchorComponent, popupAnchorPoint, rangeToHighlight,
        commandName = RefactoringBundle.message("suggested.refactoring.rename.command.name"),
        doRefactoring = { doRefactor() },
        onEnter = ::doRefactor,
        isEnterEnabled = { true },
        isEscapeEnabled = { true },
        onClosed = { isOk ->
          if (!isOk) {
            SuggestedRefactoringFeatureUsage.logEvent(SuggestedRefactoringFeatureUsage.POPUP_CANCELED, refactoringData, state, actionPlace)
          }
        }
      )
      popup.onRefactor = { callbacks.onOk(Unit) }

      SuggestedRefactoringFeatureUsage.logEvent(SuggestedRefactoringFeatureUsage.POPUP_SHOWN, refactoringData, state, actionPlace)
    }

    is SuggestedChangeSignatureData -> {
      fun doRefactor(newParameterValues: List<NewParameterValue>) {
        doRefactor(refactoringData, state, editor, actionPlace) {
          performChangeSignature(refactoringSupport, refactoringData, newParameterValues, project, editor)
        }
      }

      val newParameterData = refactoringSupport.ui.extractNewParameterData(refactoringData)

      if (!showReviewBalloon || ApplicationManager.getApplication().isHeadlessEnvironment) {
        val newParameterValues = if (ApplicationManager.getApplication().isUnitTestMode) {
          // for testing
          newParameterData.indices.map {
            _suggestedChangeSignatureNewParameterValuesForTests?.invoke(it) ?: NewParameterValue.None
          }
        }
        else {
          newParameterData.map { NewParameterValue.None }
        }
        doRefactor(newParameterValues)
        return
      }

      val presentationModel = refactoringSupport.ui.buildSignatureChangePresentation(
        refactoringData.oldSignature,
        refactoringData.newSignature
      )

      val screenSize = editor.component.graphicsConfiguration.device.defaultConfiguration.bounds.size

      val component = ChangeSignaturePopup(
        presentationModel,
        refactoringData.nameOfStuffToUpdate,
        newParameterData,
        project,
        refactoringSupport,
        refactoringData.declaration.language,
        editor.colorsScheme,
        screenSize
      )

      val rangeToHighlight = state.refactoringSupport.signatureRange(refactoringData.declaration)!!

      val callbacks = createAndShowBalloon(
        component, project, editor, popupAnchorComponent, popupAnchorPoint, rangeToHighlight,
        commandName = RefactoringBundle.message("suggested.refactoring.change.signature.command.name", refactoringData.nameOfStuffToUpdate),
        doRefactoring = ::doRefactor,
        onEnter = component::onEnter,
        isEnterEnabled = component::isEnterEnabled,
        isEscapeEnabled = component::isEscapeEnabled,
        onClosed = { isOk ->
          if (!isOk) {
            SuggestedRefactoringFeatureUsage.logEvent(SuggestedRefactoringFeatureUsage.POPUP_CANCELED, refactoringData, state, actionPlace)
          }
          Disposer.dispose(component)
        }
      )
      component.onOk = callbacks.onOk
      component.onNext = callbacks.onNext

      SuggestedRefactoringFeatureUsage.logEvent(SuggestedRefactoringFeatureUsage.POPUP_SHOWN, refactoringData, state, actionPlace)
    }
  }
}

private fun doRefactor(
  refactoringData: SuggestedRefactoringData,
  state: SuggestedRefactoringState,
  editor: Editor,
  actionPlace: String,
  doRefactor: () -> Unit
) {
  SuggestedRefactoringFeatureUsage.logEvent(SuggestedRefactoringFeatureUsage.REFACTORING_PERFORMED, refactoringData, state, actionPlace)

  val project = state.declaration.project
  UndoManager.getInstance(project).undoableActionPerformed(SuggestedRefactoringUndoableAction.create(editor.document, state))

  performWithDumbEditor(editor, doRefactor)

  // no refactoring availability anymore even if no usages updated
  SuggestedRefactoringProvider.getInstance(project).reset()
}

private data class BalloonCallbacks<TData>(val onOk: (TData) -> Unit, val onNext: () -> Unit)

private fun <TData> createAndShowBalloon(
  content: JComponent,
  project: Project,
  editor: Editor,
  popupAnchorComponent: JComponent?,
  popupAnchorPoint: Point?,
  rangeToHighlight: TextRange,
  commandName: String,
  doRefactoring: (TData) -> Unit,
  onEnter: () -> Unit,
  isEnterEnabled: () -> Boolean,
  isEscapeEnabled: () -> Boolean,
  onClosed: (isOk: Boolean) -> Unit
): BalloonCallbacks<TData> {
  val builder = JBPopupFactory.getInstance()
    .createDialogBalloonBuilder(content, null)
    .setRequestFocus(true)
    .setHideOnClickOutside(true)
    .setCloseButtonEnabled(false)
    .setAnimationCycle(0)
    .setBlockClicksThroughBalloon(true)
    .setContentInsets(Insets(0, 0, 0, 0))

  val borderColor = UIManager.getColor("InplaceRefactoringPopup.borderColor")
  if (borderColor != null) {
    builder.setBorderColor(borderColor)
  }

  val balloon = builder.createBalloon()

  positionAndShowBalloon(balloon, popupAnchorComponent, popupAnchorPoint, editor, rangeToHighlight)

  fun hideBalloonAndRefactor(data: TData) {
    balloon.hide(true)

    executeCommand(project, name = commandName, command = { doRefactoring(data) })
  }

  object : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
      onEnter()
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = isEnterEnabled()
    }
  }.registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), content, balloon)

  object : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
      balloon.hide(false)
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = isEscapeEnabled()
    }
  }.registerCustomShortcutSet(CustomShortcutSet.fromString("ESCAPE"), content, balloon)

  val attributes = TextAttributes(
    null,
    editor.colorsScheme.getColor(EditorColors.CARET_ROW_COLOR),
    null,
    null,
    Font.PLAIN
  )
  val highlighter = editor.markupModel.addRangeHighlighter(
    rangeToHighlight.startOffset,
    rangeToHighlight.endOffset,
    HighlighterLayer.FIRST,
    attributes,
    HighlighterTargetArea.LINES_IN_RANGE
  )

  LaterInvocator.enterModal(balloon)

  Disposer.register(balloon, Disposable {
    LaterInvocator.leaveModal(balloon)
    editor.markupModel.removeHighlighter(highlighter)
  })

  balloon.addListener(object : JBPopupListener {
    override fun onClosed(event: LightweightWindowEvent) {
      onClosed(event.isOk)
    }
  })

  return BalloonCallbacks(
    onOk = ::hideBalloonAndRefactor,
    onNext = { balloon.revalidate() }
  )
}

private fun positionAndShowBalloon(
  balloon: Balloon,
  popupAnchorComponent: JComponent?,
  popupAnchorPoint: Point?,
  editor: Editor,
  signatureRange: TextRange
) {
  if (popupAnchorComponent != null && popupAnchorPoint != null) {
    balloon.show(RelativePoint(popupAnchorComponent, popupAnchorPoint), Balloon.Position.below)
  }
  else {
    val top = RelativePoint(editor.contentComponent, editor.offsetToXY(signatureRange.startOffset))
    val bottom = RelativePoint(
      editor.contentComponent,
      editor.offsetToXY(signatureRange.endOffset).apply { y += editor.lineHeight })
    val caretXY = editor.offsetToXY(editor.caretModel.offset)

    val screenSize = Toolkit.getDefaultToolkit().screenSize
    if (top.screenPoint.y > screenSize.height - bottom.screenPoint.y) {
      balloon.show(
        RelativePoint(editor.contentComponent, Point(caretXY.x, top.originalPoint.y)),
        Balloon.Position.above
      )
    }
    else {
      balloon.show(
        RelativePoint(editor.contentComponent, Point(caretXY.x, bottom.originalPoint.y)),
        Balloon.Position.below
      )
    }
  }
}

private fun performWithDumbEditor(editor: Editor, action: () -> Unit) {
  (editor as? EditorImpl)?.startDumb()
  try {
    action()
  }
  finally {
    (editor as? EditorImpl)?.stopDumbLater()
  }
}

private fun performRename(refactoringSupport: SuggestedRefactoringSupport, data: SuggestedRenameData, project: Project, editor: Editor) {
  val relativeCaretOffset = editor.caretModel.offset - refactoringSupport.anchorOffset(data.declaration)

  val newName = data.declaration.name!!
  runWriteAction {
    data.declaration.setName(data.oldName)
  }

  val textOccurrencesEnabled = TextOccurrencesUtil.isSearchTextOccurrencesEnabled(data.declaration)

  val refactoring = RefactoringFactory.getInstance(project).createRename(data.declaration, newName, true, textOccurrencesEnabled)
  refactoring.respectAllAutomaticRenames()

  if (refactoring.hasNonCodeUsages() && !ApplicationManager.getApplication().isHeadlessEnvironment) {
    val question = RefactoringBundle.message("suggested.refactoring.rename.text.occurrences", data.oldName, newName)
    val result = Messages.showOkCancelDialog(
      project,
      question,
      RefactoringBundle.message("suggested.refactoring.rename.text.occurrences.title"),
      RefactoringBundle.message("suggested.refactoring.rename.with.preview.button.text"),
      RefactoringBundle.message("suggested.refactoring.ignore.button.text"),
      Messages.getQuestionIcon()
    )
    if (result != Messages.OK) {
      refactoring.isSearchInComments = false
      refactoring.isSearchInNonJavaFiles = false
    }
  }

  refactoring.run()

  if (data.declaration.isValid) {
    editor.caretModel.moveToOffset(relativeCaretOffset + refactoringSupport.anchorOffset(data.declaration))
  }
}

private fun performChangeSignature(
  refactoringSupport: SuggestedRefactoringSupport,
  data: SuggestedChangeSignatureData,
  newParameterValues: List<NewParameterValue>,
  project: Project,
  editor: Editor
) {
  val preparedData = refactoringSupport.execution.prepareChangeSignature(data)

  val relativeCaretOffset = editor.caretModel.offset - refactoringSupport.anchorOffset(data.declaration)

  val restoreNewSignature = runWriteAction {
    data.restoreInitialState(refactoringSupport)
  }

  refactoringSupport.execution.performChangeSignature(data, newParameterValues, preparedData)

  runWriteAction {
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
    restoreNewSignature()
    editor.caretModel.moveToOffset(relativeCaretOffset + refactoringSupport.anchorOffset(data.declaration))
  }
}

private fun SuggestedRefactoringSupport.anchorOffset(declaration: PsiElement): Int {
  return nameRange(declaration)?.startOffset ?: declaration.startOffset
}

@set:TestOnly
var _suggestedChangeSignatureNewParameterValuesForTests: ((index: Int) -> NewParameterValue)? = null
