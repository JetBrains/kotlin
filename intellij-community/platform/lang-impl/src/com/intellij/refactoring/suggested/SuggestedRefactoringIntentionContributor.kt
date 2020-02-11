// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.suggested

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.IntentionMenuContributor
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringBundle

class SuggestedRefactoringIntentionContributor : IntentionMenuContributor {
    private val icon = AllIcons.Actions.SuggestedRefactoringBulb

    override fun collectActions(
        hostEditor: Editor,
        hostFile: PsiFile,
        intentions: ShowIntentionsPass.IntentionsInfo,
        passIdToShowIntentionsFor: Int,
        offset: Int
    ) {
        val project = hostFile.project
        val refactoringProvider = SuggestedRefactoringProviderImpl.getInstance(project)
        var state = refactoringProvider.state ?: return

        val declaration = state.declaration
        if (!declaration.isValid) return
        if (hostFile != declaration.containingFile) return
        if (state.syntaxError) return

        val refactoringSupport = state.refactoringSupport

        state = refactoringSupport.availability.refineSignaturesWithResolve(state)

        if (state.oldSignature == state.newSignature) {
            val document = PsiDocumentManager.getInstance(project).getDocument(hostFile)!!
            val modificationStamp = document.modificationStamp
            ApplicationManager.getApplication().invokeLater {
                if (document.modificationStamp == modificationStamp) {
                    refactoringProvider.availabilityIndicator.clear()
                }
            }
            return
        }

        val refactoringData = refactoringSupport.availability.detectAvailableRefactoring(state)

        // update availability indicator with more precise state that takes into account resolve
        refactoringProvider.availabilityIndicator.update(declaration, refactoringData, refactoringSupport)

        val range = when (refactoringData) {
            is SuggestedRenameData -> refactoringSupport.nameRange(refactoringData.declaration)!!
            is SuggestedChangeSignatureData -> refactoringSupport.changeSignatureAvailabilityRange(declaration)!!
            else -> return
        }

        if (!range.containsOffset(offset)) return

        SuggestedRefactoringFeatureUsage.refactoringSuggested(refactoringData, state)

        val text = when (refactoringData) {
            is SuggestedRenameData -> RefactoringBundle.message("suggested.refactoring.rename.intention.text", refactoringData.oldName, refactoringData.declaration.name)
            is SuggestedChangeSignatureData -> RefactoringBundle.message("suggested.refactoring.change.signature.intention.text", refactoringData.nameOfStuffToUpdate)
        }

        val intention = MyIntention(text, showReviewBalloon = refactoringData is SuggestedChangeSignatureData)
        // we add it into 'errorFixesToShow' if it's not empty to always be at the top of the list
        // we don't add into it if it's empty to keep the color of the bulb
        val collectionToAdd = intentions.errorFixesToShow.takeIf { it.isNotEmpty() }
                              ?: intentions.inspectionFixesToShow
        collectionToAdd.add(0, HighlightInfo.IntentionActionDescriptor(intention, icon))
    }

    private class MyIntention(
        private val text: String,
        private val showReviewBalloon: Boolean
    ) : IntentionAction, PriorityAction {
        override fun getPriority() = PriorityAction.Priority.TOP

        override fun getFamilyName() = "Suggested Refactoring"

        override fun getText() = text

        override fun isAvailable(project: Project, editor: Editor, file: PsiFile?) = true

        override fun startInWriteAction() = false

        override fun invoke(project: Project, editor: Editor, file: PsiFile?) {
            performSuggestedRefactoring(project, editor, null, null, showReviewBalloon, ActionPlaces.INTENTION_MENU)
        }
    }
}