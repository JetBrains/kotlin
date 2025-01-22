/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript


/**
 * Special key to be associated with [KtFile] to determine the underlying nature of frontend representation
 * for the file:
 *  - either [org.jetbrains.kotlin.fir.declarations.FirReplSnippet]
 *  - or [org.jetbrains.kotlin.fir.declarations.FirScript]
 */
private val REPL_SNIPPET_ENTITY_KEY = Key.create<Boolean>("REPL_SNIPPET_SCRIPT_FILE")

/**
 * Detects if this particular file should be treated
 * as [org.jetbrains.kotlin.fir.declarations.FirReplSnippet] or [org.jetbrains.kotlin.fir.declarations.FirScript].
 */
fun KtFile.isReplSnippet(): Boolean {
    if (isScript()) return false

    return getUserData(REPL_SNIPPET_ENTITY_KEY) ?: false
}

fun KtScript.isReplSnippet(): Boolean {
    return containingKtFile.isReplSnippet()
}

fun PsiFile.isReplSnippet(): Boolean {
    return when (this) {
        is KtFile -> isReplSnippet()
        else -> false
    }
}

/**
 * Makes the file to be treated as [org.jetbrains.kotlin.fir.declarations.FirReplSnippet]
 * in all FIR related matters, especially in 'analysis-api'.
 */
fun KtFile.markAsReplSnippet(isSnippet: Boolean = true) {
    containingKtFile.putUserData(REPL_SNIPPET_ENTITY_KEY, isSnippet)
}