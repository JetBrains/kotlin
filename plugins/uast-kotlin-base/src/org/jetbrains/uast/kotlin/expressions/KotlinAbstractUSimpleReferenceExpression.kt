/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.UastVisitor

abstract class KotlinAbstractUSimpleReferenceExpression(
    override val sourcePsi: KtSimpleNameExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), USimpleNameReferenceExpression, KotlinUElementWithType, KotlinEvaluatableUElement {
    protected val resolvedDeclaration: PsiElement? by lz { baseResolveProviderService.resolveToDeclaration(sourcePsi) }

    override val identifier get() = sourcePsi.getReferencedName()

    override fun resolve() = resolvedDeclaration

    override val resolvedName: String?
        get() = (resolvedDeclaration as? PsiNamedElement)?.name

    override fun accept(visitor: UastVisitor) {
        visitor.visitSimpleNameReferenceExpression(this)

        uAnnotations.acceptList(visitor)

        visitor.afterVisitSimpleNameReferenceExpression(this)
    }

    override val referenceNameElement: UElement? by lz { sourcePsi.getIdentifier()?.toUElement() }
}
