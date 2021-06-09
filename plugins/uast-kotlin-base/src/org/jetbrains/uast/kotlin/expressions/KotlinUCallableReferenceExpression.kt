/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.ResolveResult
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.internal.getResolveResultVariants

class KotlinUCallableReferenceExpression(
    override val sourcePsi: KtCallableReferenceExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UCallableReferenceExpression, UMultiResolvable, KotlinUElementWithType {
    override val qualifierExpression: UExpression?
        get() {
            if (qualifierType != null) return null
            val receiverExpression = sourcePsi.receiverExpression ?: return null
            return baseResolveProviderService.baseKotlinConverter.convertExpression(receiverExpression, this, DEFAULT_EXPRESSION_TYPES_LIST)
        }

    override val qualifierType by lz {
        baseResolveProviderService.getDoubleColonReceiverType(sourcePsi, this)
    }

    override val callableName: String
        get() = sourcePsi.callableReference.getReferencedName()

    override val resolvedName: String?
        get() = (resolve() as? PsiNamedElement)?.name

    override fun resolve(): PsiElement? = baseResolveProviderService.resolveToDeclaration(sourcePsi.callableReference)

    override fun multiResolve(): Iterable<ResolveResult> =
        getResolveResultVariants(baseResolveProviderService, sourcePsi.callableReference)

}
