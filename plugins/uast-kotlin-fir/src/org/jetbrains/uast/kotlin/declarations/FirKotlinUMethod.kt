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
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.uast.*

open class FirKotlinUMethod(
    psi: PsiMethod,
    sourcePsi: KtDeclaration?,
    givenParent: UElement?
) : BaseKotlinUMethod(psi, sourcePsi, givenParent) {
    constructor(
        psi: KtLightMethod,
        givenParent: UElement?
    ) : this(psi, getKotlinMemberOrigin(psi), givenParent)

    override val uastParameters: List<UParameter> by lz {
        val lightParams = psi.parameterList.parameters
        val receiverTypeReference =
            receiverTypeReference ?: return@lz lightParams.map { FirKotlinUParameter(it, getKotlinMemberOrigin(it), this) }
        val lightReceiver = lightParams.firstOrNull() ?: return@lz emptyList<UParameter>()
        val uParameters = SmartList<UParameter>(FirKotlinReceiverUParameter(lightReceiver, receiverTypeReference, this))
        lightParams.drop(1).mapTo(uParameters) { FirKotlinUParameter(it, getKotlinMemberOrigin(it), this) }
        uParameters
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

// TODO: can be commonized if *KotlinUMethod is commonized
open class FirKotlinConstructorUMethod(
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

    override val uastBody: UExpression? by lz {
        val delegationCall: KtCallElement? = sourcePsi.let {
            when {
                isPrimary -> ktClass?.superTypeListEntries?.firstIsInstanceOrNull<KtSuperTypeCallEntry>()
                it is KtSecondaryConstructor -> it.getDelegationCall()
                else -> null
            }
        }
        val bodyExpressions = getBodyExpressions()
        if (delegationCall == null && bodyExpressions.isEmpty()) return@lz null
        KotlinLazyUBlockExpression(this) { uastParent ->
            SmartList<UExpression>().apply {
                delegationCall?.let {
                    // TODO: function call for delegationCall
                    add(UastEmptyExpression(uastParent))
                }
                bodyExpressions.forEach {
                    add(baseResolveProviderService.baseKotlinConverter.convertOrEmpty(it, uastParent))
                }
            }
        }
    }

    override val uastAnchor: UIdentifier? by lz {
        KotlinUIdentifier(
            javaPsi.nameIdentifier,
            if (isPrimary) ktClass?.nameIdentifier else (sourcePsi as? KtSecondaryConstructor)?.getConstructorKeyword(),
            this
        )
    }

    protected open fun getBodyExpressions(): List<KtExpression> {
        if (isPrimary) return getInitializers()
        val bodyExpression = (sourcePsi as? KtFunction)?.bodyExpression ?: return emptyList()
        if (bodyExpression is KtBlockExpression) return bodyExpression.statements
        return listOf(bodyExpression)
    }

    protected fun getInitializers(): List<KtExpression> {
        return ktClass?.getAnonymousInitializers()?.mapNotNull { it.body } ?: emptyList()
    }
}

// TODO: can be commonized if *KotlinUMethod is commonized
//   also reuse the comments there (about KT-21617)
class FirKotlinSecondaryConstructorWithInitializersUMethod(
    ktClass: KtClassOrObject?,
    psi: KtLightMethod,
    givenParent: UElement?
) : FirKotlinConstructorUMethod(ktClass, psi, givenParent) {
    override fun getBodyExpressions(): List<KtExpression> {
        return getInitializers() + super.getBodyExpressions()
    }
}
