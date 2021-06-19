/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.uast.*

class FirKotlinUMethod(
    psi: PsiMethod,
    sourcePsi: KtDeclaration?,
    givenParent: UElement?
) : BaseKotlinUMethod(psi, sourcePsi, givenParent), FirKotlinUMethodParametersProducer {
    constructor(
        psi: KtLightMethod,
        givenParent: UElement?
    ) : this(psi, getKotlinMemberOrigin(psi), givenParent)

    override val uastParameters: List<UParameter> by lz { produceUastParameters(this, receiverTypeReference) }

    companion object {
        fun create(
            psi: KtLightMethod,
            givenParent: UElement?
        ): UMethod {
            val kotlinOrigin = psi.kotlinOrigin
            return when {
                kotlinOrigin is KtConstructor<*> ->
                    FirKotlinConstructorUMethod(kotlinOrigin.containingClassOrObject, psi, givenParent)
                kotlinOrigin is KtParameter && kotlinOrigin.getParentOfType<KtClass>(true)?.isAnnotation() == true ->
                    FirKotlinUAnnotationMethod(psi, givenParent)
                else ->
                    FirKotlinUMethod(psi, givenParent)
            }
        }

        fun create(
            sourcePsi: KtDeclaration?,
            givenParent: UElement?
        ): UMethod? {
            val javaPsi = when (sourcePsi) {
                is KtPropertyAccessor ->
                    LightClassUtil.getLightClassAccessorMethod(sourcePsi)
                is KtFunction ->
                    LightClassUtil.getLightClassMethod(sourcePsi)
                else -> null
            } as? KtLightMethod ?: return null
            return create(javaPsi, givenParent)
        }
    }
}
