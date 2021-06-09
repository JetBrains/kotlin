/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin.internal

import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.ResolveResult
import com.intellij.psi.infos.CandidateInfo
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.uast.kotlin.BaseKotlinUastResolveProviderService

fun getResolveResultVariants(
    baseKotlinUastResolveProviderService: BaseKotlinUastResolveProviderService,
    ktExpression: KtExpression?
): Iterable<ResolveResult> {
    ktExpression ?: return emptyList()

    val referenceVariants = baseKotlinUastResolveProviderService.getReferenceVariants(ktExpression, ktExpression.name ?: ktExpression.text)

    return referenceVariants.mapNotNull { CandidateInfo(it, PsiSubstitutor.EMPTY) }.asIterable()
}

