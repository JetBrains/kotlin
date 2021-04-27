/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.uast.*

open class FirKotlinUMethod(
    psi: PsiMethod,
    final override val sourcePsi: KtDeclaration?,
    givenParent: UElement?
) : FirKotlinAbstractUElement(givenParent), UMethod, UAnchorOwner, PsiMethod by psi {
    constructor(
        psi: KtLightMethod,
        givenParent: UElement?
    ) : this(psi, getKotlinMemberOrigin(psi), givenParent)

    override val psi: PsiMethod = unwrap<UMethod, PsiMethod>(psi)

    override val javaPsi = psi

    override fun getSourceElement() = sourcePsi

    override val uAnnotations: List<UAnnotation>
        get() {
            // TODO: Not yet implemented
            return emptyList()
        }

    private val receiverTypeReference by lz {
        when (sourcePsi) {
            is KtCallableDeclaration -> sourcePsi
            is KtPropertyAccessor -> sourcePsi.property
            else -> null
        }?.receiverTypeReference
    }

    override val uastParameters: List<UParameter> by lz {
        val lightParams = psi.parameterList.parameters
        val receiverTypeReference =
            receiverTypeReference ?: return@lz lightParams.map { FirKotlinUParameter(it, getKotlinMemberOrigin(it), this) }
        val lightReceiver = lightParams.firstOrNull() ?: return@lz emptyList<UParameter>()
        val uParameters = SmartList<UParameter>(FirKotlinReceiverUParameter(lightReceiver, receiverTypeReference, this))
        lightParams.drop(1).mapTo(uParameters) { FirKotlinUParameter(it, getKotlinMemberOrigin(it), this) }
        uParameters
    }

    override val uastAnchor: UIdentifier? by lz {
        val identifierSourcePsi = when (val sourcePsi = sourcePsi) {
            is PsiNameIdentifierOwner -> sourcePsi.nameIdentifier
            is KtObjectDeclaration -> sourcePsi.getObjectKeyword()
            is KtPropertyAccessor -> sourcePsi.namePlaceholder
            else -> sourcePsi?.navigationElement
        }
        FirKotlinUIdentifier(nameIdentifier, identifierSourcePsi, this)
    }

    override val uastBody: UExpression? by lz {
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

        UastFacade.findPlugin(this)?.convertElement(bodyExpression, this) as? UExpression
    }

    override val returnTypeReference: UTypeReferenceExpression? by lz {
        (sourcePsi as? KtCallableDeclaration)?.typeReference?.let {
            FirKotlinUTypeReferenceExpression(it, this) { javaPsi.returnType ?: UastErrorType }
        }
    }

    companion object {
        fun create(
            psi: KtLightMethod,
            givenParent: UElement?
        ): FirKotlinUMethod {
            return when (psi) {
                is KtConstructor<*> ->
                    FirKotlinConstructorUMethod(psi.containingClassOrObject, psi, givenParent)
                // TODO: FirKotlinUAnnotationMethod
                else ->
                    FirKotlinUMethod(psi, givenParent)
            }
        }

        fun create(
            sourcePsi: KtDeclaration?,
            givenParent: UElement?
        ): FirKotlinUMethod? {
            val javaPsi = when (sourcePsi) {
                is KtPropertyAccessor ->
                    LightClassUtil.getLightClassAccessorMethod(sourcePsi)
                is KtFunction ->
                    LightClassUtil.getLightClassMethod(sourcePsi)
                else -> null
            } ?: return null
            return when (sourcePsi) {
                is KtConstructor<*> ->
                    FirKotlinConstructorUMethod(sourcePsi.containingClassOrObject, javaPsi, sourcePsi, givenParent)
                // TODO: FirKotlinUAnnotationMethod
                else ->
                    FirKotlinUMethod(javaPsi, sourcePsi, givenParent)
            }
        }
    }
}

class FirKotlinConstructorUMethod(
    private val ktClass: KtClassOrObject?,
    override val psi: PsiMethod,
    kotlinOrigin: KtDeclaration?,
    givenParent: UElement?
) : FirKotlinUMethod(psi, kotlinOrigin, givenParent) {
    constructor(
        ktClass: KtClassOrObject?,
        psi: KtLightMethod,
        givenParent: UElement?
    ) : this(ktClass, psi, psi.kotlinOrigin, givenParent)

    override val javaPsi = psi

    val isPrimary: Boolean
        get() = sourcePsi is KtPrimaryConstructor || sourcePsi is KtClassOrObject

    override val uastAnchor: UIdentifier? by lz {
        FirKotlinUIdentifier(
            javaPsi.nameIdentifier,
            if (isPrimary) ktClass?.nameIdentifier else (sourcePsi as? KtSecondaryConstructor)?.getConstructorKeyword(),
            this
        )
    }
}
