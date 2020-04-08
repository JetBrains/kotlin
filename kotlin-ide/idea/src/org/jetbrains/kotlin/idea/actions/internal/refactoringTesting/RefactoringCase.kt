/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.internal.refactoringTesting

import com.intellij.openapi.project.Project

internal sealed class RandomMoveRefactoringResult {
    internal class Success(val caseData: String) : RandomMoveRefactoringResult()
    internal class ExceptionCaused(val caseData: String, val message: String) : RandomMoveRefactoringResult()
    internal companion object Failed : RandomMoveRefactoringResult()
}

internal interface RefactoringCase {
    fun tryCreateAndRun(project: Project): RandomMoveRefactoringResult
}

internal fun RefactoringCase.tryCreateAndRun(project: Project, refactoringCountBeforeCheck: Int): RandomMoveRefactoringResult {

    require(refactoringCountBeforeCheck >= 1) { "refactoringCountBeforeCheck must be greater than zero" }

    if (refactoringCountBeforeCheck == 1)
        return tryCreateAndRun(project)

    var maxTries = refactoringCountBeforeCheck * 10

    var exception: RandomMoveRefactoringResult.ExceptionCaused? = null
    val refactoringSequence = generateSequence {

        if (maxTries-- < 0) return@generateSequence null

        val refactoringResult = tryCreateAndRun(project)

        if (refactoringResult is RandomMoveRefactoringResult.ExceptionCaused) {
            exception = refactoringResult
            return@generateSequence null
        }

        refactoringResult
    }

    val caseHistory = refactoringSequence
        .filterIsInstance<RandomMoveRefactoringResult.Success>()
        .take(refactoringCountBeforeCheck)
        .map { it.caseData }
        .foldIndexed("") { index, acc, argument -> "$acc\n\nRefactoring #${index + 1}:\n$argument" }


    exception?.run {
        val exceptionCaseData =
            if (caseHistory.isNotEmpty()) "Refactorings sequence:\n$caseHistory\ncaused an exception on further refactoring\n$caseData"
            else caseData

        RandomMoveRefactoringResult.ExceptionCaused(exceptionCaseData, message)
    }?.let { return it }

    return if (caseHistory.isNotEmpty()) RandomMoveRefactoringResult.Success(caseHistory)
    else RandomMoveRefactoringResult.Failed
}