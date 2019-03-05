/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.internal

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult
import com.intellij.psi.infos.CandidateInfo
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UMultiResolvable
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.kotlin.KotlinUastResolveProviderService
import org.jetbrains.uast.kotlin.getMaybeLightElement
import org.jetbrains.uast.kotlin.toSource


internal fun getReferenceVariants(ktElement: KtElement, nameHint: String): Sequence<DeclarationDescriptor> =
    ServiceManager.getService(ktElement.project, KotlinUastResolveProviderService::class.java).getReferenceVariants(ktElement, nameHint)

internal fun UElement.getResolveResultVariants(ktExpression: KtExpression?): Iterable<ResolveResult> {
    ktExpression ?: return emptyList()

    if (!Registry.`is`("kotlin.uast.multiresolve.enabled", true)) return ktExpression.multiResolveResults().asIterable()

    val referenceVariants = getReferenceVariants(ktExpression, ktExpression.name ?: ktExpression.text)

    fun asCandidateInfo(descriptor: DeclarationDescriptor): CandidateInfo? =
        descriptor.toSource()?.getMaybeLightElement()?.let { CandidateInfo(it, PsiSubstitutor.EMPTY) }

    return referenceVariants.mapNotNull(::asCandidateInfo).asIterable()
}


internal fun KtElement.multiResolveResults(): Sequence<ResolveResult> =
    references.asSequence().flatMap { ref ->
        when (ref) {
            is PsiPolyVariantReference -> ref.multiResolve(false).asSequence()
            else -> (ref.resolve()?.let { sequenceOf(CandidateInfo(it, PsiSubstitutor.EMPTY)) }).orEmpty()
        }
    }

interface DelegatedMultiResolve : UMultiResolvable, UResolvable {
    override fun multiResolve(): Iterable<ResolveResult> = listOfNotNull(resolve()?.let { CandidateInfo(it, PsiSubstitutor.EMPTY) })
}

class TypedResolveResult<T : PsiElement>(element: T) : CandidateInfo(element, PsiSubstitutor.EMPTY) {
    @Suppress("UNCHECKED_CAST")
    override fun getElement(): T = super.getElement() as T
}

