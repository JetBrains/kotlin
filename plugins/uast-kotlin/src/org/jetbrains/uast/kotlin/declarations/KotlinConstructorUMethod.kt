/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression

class KotlinConstructorUMethod(
    ktClass: KtClassOrObject?,
    psi: PsiMethod,
    kotlinOrigin: KtDeclaration?,
    givenParent: UElement?
) : BaseKotlinConstructorUMethod(ktClass, psi, kotlinOrigin, givenParent), KotlinUMethodParametersProducer {
    constructor(
        ktClass: KtClassOrObject?,
        psi: KtLightMethod,
        givenParent: UElement?
    ) : this(ktClass, psi, psi.kotlinOrigin, givenParent)

    override val uastParameters by lz { produceUastParameters(this, receiverTypeReference) }

    override fun buildDelegationCall(delegationCall: KtCallElement, uastParent: UElement): UExpression {
        return KotlinUFunctionCallExpression(delegationCall, uastParent)
    }
}
