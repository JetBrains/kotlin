/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.isGetter
import org.jetbrains.kotlin.asJava.elements.isSetter
import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.*

abstract class BaseKotlinUMethod(
    psi: PsiMethod,
    final override val sourcePsi: KtDeclaration?,
    givenParent: UElement?
) : KotlinAbstractUElement(givenParent), UMethod, UAnchorOwner, PsiMethod by psi {

    override val psi: PsiMethod = unwrap<UMethod, PsiMethod>(psi)

    override val javaPsi = psi

    override fun getSourceElement() = sourcePsi ?: this

    private val kotlinOrigin = getKotlinMemberOrigin(psi.originalElement) ?: sourcePsi

    override fun getContainingFile(): PsiFile? {
        kotlinOrigin?.containingFile?.let { return it }
        return unwrapFakeFileForLightClass(psi.containingFile)
    }

    override fun getNameIdentifier() = UastLightIdentifier(psi, kotlinOrigin)

    override val uAnnotations: List<UAnnotation> by lz {
        psi.annotations
            .mapNotNull { (it as? KtLightElement<*, *>)?.kotlinOrigin as? KtAnnotationEntry }
            .map { baseResolveProviderService.baseKotlinConverter.convertAnnotation(it, this) }
    }

    protected val receiverTypeReference by lz {
        when (sourcePsi) {
            is KtCallableDeclaration -> sourcePsi
            is KtPropertyAccessor -> sourcePsi.property
            else -> null
        }?.receiverTypeReference
    }

    override val uastAnchor: UIdentifier? by lz {
        val identifierSourcePsi = when (val sourcePsi = sourcePsi) {
            is PsiNameIdentifierOwner -> sourcePsi.nameIdentifier
            is KtObjectDeclaration -> sourcePsi.getObjectKeyword()
            is KtPropertyAccessor -> sourcePsi.namePlaceholder
            else -> sourcePsi?.navigationElement
        }
        KotlinUIdentifier(nameIdentifier, identifierSourcePsi, this)
    }

    override val uastBody: UExpression? by lz {
        if (kotlinOrigin?.canAnalyze() != true) return@lz null // EA-137193
        val bodyExpression = when (sourcePsi) {
            is KtFunction -> sourcePsi.bodyExpression
            is KtPropertyAccessor -> sourcePsi.bodyExpression
            is KtProperty -> when {
                psi is KtLightMethod && psi.isGetter -> sourcePsi.getter?.bodyExpression
                psi is KtLightMethod && psi.isSetter -> sourcePsi.setter?.bodyExpression
                else -> null
            }
            else -> null
        } ?: return@lz null

        wrapExpressionBody(this, bodyExpression)
    }

    override val returnTypeReference: UTypeReferenceExpression? by lz {
        (sourcePsi as? KtCallableDeclaration)?.typeReference?.let {
            KotlinUTypeReferenceExpression(it, this) { javaPsi.returnType ?: UastErrorType }
        }
    }
}
