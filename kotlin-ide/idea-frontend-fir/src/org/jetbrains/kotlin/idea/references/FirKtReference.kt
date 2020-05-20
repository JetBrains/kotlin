/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.idea.fir.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.fir.AnalysisSessionFirImpl

interface FirKtReference : KtReference {
    fun getResolvedToPsi(
        analysisSession: AnalysisSessionFirImpl,
        session: FirSession,
        state: FirModuleResolveState
    ): Collection<PsiElement>

    override val resolver get() = KtFirReferenceResolver
}