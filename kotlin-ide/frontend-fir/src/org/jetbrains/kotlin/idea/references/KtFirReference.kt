/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtSymbolBasedReference
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol

interface KtFirReference : KtReference, KtSymbolBasedReference {
    fun getResolvedToPsi(analysisSession: KtAnalysisSession): Collection<PsiElement> =
        analysisSession.resolveToSymbols().mapNotNull(KtSymbol::psi)

    override val resolver get() = KtFirReferenceResolver
}