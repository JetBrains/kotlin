// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.suggested

import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager

class SuggestedRefactoringUndoableAction private constructor(
  document: Document,
  private val project: Project,
  private val signatureRange: TextRange,
  private val oldDeclarationText: String,
  private val oldImportsText: String?,
  private val oldSignature: SuggestedRefactoringSupport.Signature,
  private val newSignature: SuggestedRefactoringSupport.Signature,
  private val disappearedParameters: Map<String, Any>,
  private val additionalData: SuggestedRefactoringState.AdditionalData
) : UndoableAction
{
  companion object {
    fun create(document: Document, state: SuggestedRefactoringState): SuggestedRefactoringUndoableAction {
      val signatureRange = state.refactoringSupport.signatureRange(state.declaration)!!
      return SuggestedRefactoringUndoableAction(
        document, state.declaration.project, signatureRange, state.oldDeclarationText, state.oldImportsText,
        state.oldSignature, state.newSignature, state.disappearedParameters, state.additionalData
      )
    }
  }

  private val documentReference = DocumentReferenceManager.getInstance().create(document)

  override fun getAffectedDocuments() = arrayOf(documentReference)

  override fun isGlobal() = false

  override fun undo() {
    val document = documentReference.document ?: return
    val psiDocumentManager = PsiDocumentManager.getInstance(project)
    psiDocumentManager.commitAllDocuments()
    val psiFile = psiDocumentManager.getPsiFile(document) ?: return
    val refactoringSupport = SuggestedRefactoringSupport.forLanguage(psiFile.language) ?: return
    val declaration = refactoringSupport.declarationByOffset(psiFile, signatureRange.startOffset)
                        ?.takeIf { refactoringSupport.signatureRange(it) == signatureRange } ?: return
    val state = SuggestedRefactoringState(
      declaration, refactoringSupport, SuggestedRefactoringState.ErrorLevel.NO_ERRORS,
      oldDeclarationText, oldImportsText, oldSignature, newSignature,
      refactoringSupport.stateChanges.parameterMarkers(declaration, newSignature), disappearedParameters, additionalData = additionalData
    )

    SuggestedRefactoringProviderImpl.getInstance(project).undoToState(state, signatureRange)
  }

  override fun redo() {
    SuggestedRefactoringProvider.getInstance(project).reset()
  }
}