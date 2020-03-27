// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.suggested

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.SuggestedRefactoringState.ErrorLevel
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.annotations.TestOnly

class SuggestedRefactoringChangeCollector(
  private val availabilityIndicator: SuggestedRefactoringAvailabilityIndicator
) : SuggestedRefactoringSignatureWatcher {

  @Volatile var state: SuggestedRefactoringState? = null
    private set

  override fun editingStarted(declaration: PsiElement, refactoringSupport: SuggestedRefactoringSupport) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    require(declaration.isValid)
    state = refactoringSupport.stateChanges.createInitialState(declaration)
    updateAvailabilityIndicator()
    amendStateInBackground()

    val project = declaration.project
    val document = PsiDocumentManager.getInstance(project).getDocument(declaration.containingFile)!!
    // add UndoableAction which will reset signature tracking state to initial
    UndoManager.getInstance(project).undoableActionPerformed(EditingStartedUndoableAction(document, project))
  }

  override fun nextSignature(declaration: PsiElement, refactoringSupport: SuggestedRefactoringSupport) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    require(declaration.isValid)
    state = state?.let { refactoringSupport.stateChanges.updateState(it, declaration) }
    updateAvailabilityIndicator()
    amendStateInBackground()
  }

  override fun inconsistentState() {
    ApplicationManager.getApplication().assertIsDispatchThread()
    state = state?.withErrorLevel(ErrorLevel.INCONSISTENT)
    updateAvailabilityIndicator()
  }

  override fun reset() {
    ApplicationManager.getApplication().assertIsDispatchThread()
    state = null
    updateAvailabilityIndicator()
  }

  fun undoToState(state: SuggestedRefactoringState) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    this.state = state
    updateAvailabilityIndicator()
    amendStateInBackground()
  }

  private fun amendStateInBackground() {
    if (!_amendStateInBackgroundEnabled) return
    var initialState = state ?: return
    val stateLock = Any()
    require(initialState.errorLevel != ErrorLevel.INCONSISTENT)
    val psiFile = initialState.declaration.containingFile
    val project = initialState.declaration.project
    val psiDocumentManager = PsiDocumentManager.getInstance(project)
    val document = psiDocumentManager.getDocument(psiFile)!!

    ReadAction.nonBlocking {
        val states = initialState.refactoringSupport.availability.amendStateInBackground(initialState)
        states.forEach { newState ->
          ApplicationManager.getApplication().invokeLater {
            synchronized(stateLock) {
              if (state !== initialState) return@invokeLater
              state = newState
              initialState = newState // update initialState so that condition of expireWhen remains false
            }
            if (psiDocumentManager.isCommitted(document)) { // we can't update availability indicator if document is not committed
              updateAvailabilityIndicator()
            }
          }
        }
      }
      .inSmartMode(project)
      .expireWhen {
        synchronized(stateLock) { state !== initialState }
      }
      .expireWith(project)
      .submit(AppExecutorUtil.getAppExecutorService())
  }

  private fun updateAvailabilityIndicator() {
    val state = this.state
    if (state == null || state.oldSignature == state.newSignature && state.errorLevel == ErrorLevel.NO_ERRORS) {
      availabilityIndicator.clear()
      return
    }

    val refactoringSupport = state.refactoringSupport
    if (!state.declaration.isValid || refactoringSupport.nameRange(state.declaration) == null) {
      availabilityIndicator.disable()
      return
    }

    val refactoringData = if (state.errorLevel == ErrorLevel.NO_ERRORS)
      refactoringSupport.availability.detectAvailableRefactoring(state)
    else
      null
    availabilityIndicator.update(state.declaration, refactoringData, refactoringSupport)
  }

  @set:TestOnly
  var _amendStateInBackgroundEnabled = !ApplicationManager.getApplication().isUnitTestMode
    set(value) {
      if (value != field) {
        field = value
        if (value && state?.errorLevel != ErrorLevel.INCONSISTENT) {
          amendStateInBackground()
        }
      }
    }

  private class EditingStartedUndoableAction(document: Document, private val project: Project) : BasicUndoableAction(document) {
    override fun undo() {
      SuggestedRefactoringProvider.getInstance(project).reset()
    }

    override fun redo() {
    }
  }
}
