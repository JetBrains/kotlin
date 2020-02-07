// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.suggested

import com.intellij.psi.PsiElement

class SuggestedRefactoringChangeCollector(
    private val availabilityIndicator: SuggestedRefactoringAvailabilityIndicator
) : SuggestedRefactoringSignatureWatcher {
    
    var state: SuggestedRefactoringState? = null
        private set

    override fun editingStarted(declaration: PsiElement, refactoringSupport: SuggestedRefactoringSupport) {
        require(declaration.isValid)
        state = refactoringSupport.stateChanges.createInitialState(declaration)
        updateAvailabilityIndicator()
    }

    override fun nextSignature(declaration: PsiElement, refactoringSupport: SuggestedRefactoringSupport) {
        require(declaration.isValid)
        state = state?.let { refactoringSupport.stateChanges.updateState(it, declaration) }
        updateAvailabilityIndicator()
    }

    override fun inconsistentState(refactoringSupport: SuggestedRefactoringSupport) {
        state = state?.copy(syntaxError = true)
        updateAvailabilityIndicator()
    }

    override fun reset() {
        state = null
        updateAvailabilityIndicator()
    }

    private fun updateAvailabilityIndicator() {
        val state = this.state
        if (state == null || state.oldSignature == state.newSignature && !state.syntaxError) {
            availabilityIndicator.clear()
            return
        }

        val refactoringSupport = state.refactoringSupport
        if (!state.declaration.isValid || refactoringSupport.nameRange(state.declaration) == null) {
            require(state.syntaxError)
            availabilityIndicator.disable()
            return
        }

        val refactoringData = if (!state.syntaxError)
            refactoringSupport.availability.detectAvailableRefactoring(state)
        else
            null
        availabilityIndicator.update(state.declaration, refactoringData, refactoringSupport)
    }
}
