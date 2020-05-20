/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.idea.fir.FirModuleResolveState
import org.jetbrains.kotlin.idea.frontend.api.VariableAsFunctionLikeCallInfo
import org.jetbrains.kotlin.idea.frontend.api.fir.AnalysisSessionFirImpl
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression

class KtInvokeFunctionReferenceFirImpl(expression: KtCallExpression) : KtInvokeFunctionReference(expression), FirKtReference {
    override fun doRenameImplicitConventionalCall(newName: String?): KtExpression {
        TODO("Not yet implemented")
    }

    override fun getResolvedToPsi(
        analysisSession: AnalysisSessionFirImpl,
        session: FirSession,
        state: FirModuleResolveState
    ): Collection<PsiElement> {
        val call = analysisSession.resolveCall(expression) ?: return emptyList()
        if (call is VariableAsFunctionLikeCallInfo) {
            return listOf(call.invokeFunction)
        }
        return emptyList()
    }
}