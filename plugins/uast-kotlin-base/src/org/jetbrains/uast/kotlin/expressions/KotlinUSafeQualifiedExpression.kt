/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.ResolveResult
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMultiResolvable
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.kotlin.internal.getResolveResultVariants

class KotlinUSafeQualifiedExpression(
    override val sourcePsi: KtSafeQualifiedExpression,
    givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UQualifiedReferenceExpression, UMultiResolvable,
    KotlinUElementWithType, KotlinEvaluatableUElement {
    override val receiver by lz { baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.receiverExpression, this) }
    override val selector by lz { baseResolveProviderService.baseKotlinConverter.convertOrEmpty(sourcePsi.selectorExpression, this) }
    override val accessType = KotlinQualifiedExpressionAccessTypes.SAFE

    override val resolvedName: String?
        get() = (resolve() as? PsiNamedElement)?.name

    override fun resolve(): PsiElement? = sourcePsi.selectorExpression?.let { baseResolveProviderService.resolveToDeclaration(it) }

    override fun multiResolve(): Iterable<ResolveResult> =
        getResolveResultVariants(baseResolveProviderService, sourcePsi.selectorExpression)
}
