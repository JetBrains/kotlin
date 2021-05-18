/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.kotlin.internal.DelegatedMultiResolve

class KotlinUImportStatement(
    override val psi: KtImportDirective,
    givenParent: UElement?
) : KotlinAbstractUElement(givenParent), UImportStatement, DelegatedMultiResolve {

    override val javaPsi: PsiElement? = null

    override val sourcePsi: KtImportDirective = psi

    override val isOnDemand: Boolean = sourcePsi.isAllUnder

    private val importRef: ImportReference? by lz {
        sourcePsi.importedReference?.let {
            ImportReference(it, sourcePsi.name ?: sourcePsi.text, this, sourcePsi)
        }
    }

    override val importReference: UElement? = importRef

    override fun resolve(): PsiElement? = importRef?.resolve()

    private class ImportReference(
        override val psi: KtExpression,
        override val identifier: String,
        givenParent: UElement?,
        private val importDirective: KtImportDirective
    ) : KotlinAbstractUExpression(givenParent), USimpleNameReferenceExpression {
        override val sourcePsi: KtExpression = psi

        override val resolvedName: String = identifier

        override fun asRenderString(): String = importDirective.importedFqName?.asString() ?: sourcePsi.text

        override fun resolve(): PsiElement? {
            val reference = sourcePsi.getQualifiedElementSelector() as? KtReferenceExpression ?: return null
            return baseResolveProviderService.resolveToDeclaration(reference)
        }
    }
}
