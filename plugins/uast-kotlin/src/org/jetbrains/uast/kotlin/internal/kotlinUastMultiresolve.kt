/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.internal

import com.intellij.openapi.components.ServiceManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult
import com.intellij.psi.infos.CandidateInfo
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.uast.kotlin.KotlinUastResolveProviderService
import org.jetbrains.uast.kotlin.resolveToDeclarationImpl


internal fun getReferenceVariants(ktElement: KtExpression, nameHint: String): Sequence<PsiElement> =
    ServiceManager.getService(ktElement.project, KotlinUastResolveProviderService::class.java)
        .getReferenceVariants(ktElement, nameHint)
        .mapNotNull { resolveToDeclarationImpl(ktElement, it) }

internal fun getResolveResultVariants(ktExpression: KtExpression?): Iterable<ResolveResult> {
    ktExpression ?: return emptyList()

    val referenceVariants = getReferenceVariants(ktExpression, ktExpression.name ?: ktExpression.text)

    return referenceVariants.mapNotNull {CandidateInfo(it, PsiSubstitutor.EMPTY) }.asIterable()
}


internal fun KtElement.multiResolveResults(): Sequence<ResolveResult> =
    references.asSequence().flatMap { ref ->
        when (ref) {
            is PsiPolyVariantReference -> ref.multiResolve(false).asSequence()
            else -> (ref.resolve()?.let { sequenceOf(CandidateInfo(it, PsiSubstitutor.EMPTY)) }).orEmpty()
        }
    }

class TypedResolveResult<T : PsiElement>(element: T) : CandidateInfo(element, PsiSubstitutor.EMPTY) {
    @Suppress("UNCHECKED_CAST")
    override fun getElement(): T = super.getElement() as T
}

