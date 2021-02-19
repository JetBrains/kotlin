/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression

abstract class KtExpressionTypeProvider : KtAnalysisSessionComponent() {
    abstract fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KtType
    abstract fun getKtExpressionType(expression: KtExpression): KtType

    abstract fun getExpectedType(expression: PsiElement): KtType?
}

interface KtExpressionTypeProviderMixIn : KtAnalysisSessionMixIn {
    fun KtExpression.getKtType(): KtType =
        analysisSession.expressionTypeProvider.getKtExpressionType(this)

    fun KtDeclaration.getReturnKtType(): KtType =
        analysisSession.expressionTypeProvider.getReturnTypeForKtDeclaration(this)

    fun PsiElement.getExpectedType(): KtType? =
        analysisSession.expressionTypeProvider.getExpectedType(this)
}
