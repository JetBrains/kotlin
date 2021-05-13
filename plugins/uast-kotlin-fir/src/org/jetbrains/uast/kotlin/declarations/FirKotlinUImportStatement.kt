/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.USimpleNameReferenceExpression

class FirKotlinUImportStatement(
    override val psi: KtImportDirective,
    givenParent: UElement?
) : KotlinAbstractUElement(givenParent), UImportStatement {
    override val javaPsi: PsiElement? = null

    override val sourcePsi: KtImportDirective = psi

    override val isOnDemand: Boolean = sourcePsi.isAllUnder

    private val importRef: FirImportReference? by lz {
        sourcePsi.importedReference?.let {
            FirImportReference(it, sourcePsi.name ?: sourcePsi.text, this, sourcePsi)
        }
    }

    override val importReference: UElement? = importRef

    override fun resolve(): PsiElement? = importRef?.resolve()

    private class FirImportReference(
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
            analyse(reference) {
                return reference.mainReference.resolve()
            }
        }
    }
}
