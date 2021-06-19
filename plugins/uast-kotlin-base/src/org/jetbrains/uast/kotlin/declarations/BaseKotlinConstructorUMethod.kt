/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UIdentifier

abstract class BaseKotlinConstructorUMethod(
    private val ktClass: KtClassOrObject?,
    override val psi: PsiMethod,
    kotlinOrigin: KtDeclaration?,
    givenParent: UElement?
) : BaseKotlinUMethod(psi, kotlinOrigin, givenParent) {

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
                    add(buildDelegationCall(it, uastParent))
                }
                bodyExpressions.forEach {
                    add(baseResolveProviderService.baseKotlinConverter.convertOrEmpty(it, uastParent))
                }
            }
        }
    }

    abstract fun buildDelegationCall(delegationCall: KtCallElement, uastParent: UElement): UExpression

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
