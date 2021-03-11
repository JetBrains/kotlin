/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.mutability

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

class CallResolver(private val fqNames: Set<FqName>) {
    private val shortNames = fqNames.map { it.shortName().identifier }.toSet()

    private fun CallableDescriptor.isMutatorCall(): Boolean {
        if (fqNameOrNull() in fqNames) return true
        return overriddenDescriptors.any { it.isMutatorCall() }
    }

    fun isNeededCall(expression: KtExpression, resolutionFacade: ResolutionFacade): Boolean {
        val shortName = when (expression) {
            is KtCallExpression -> expression.calleeExpression?.text
            is KtReferenceExpression -> expression.text
            else -> null
        } ?: return false
        if (shortName !in shortNames) return false
        val call = expression.resolveToCall(resolutionFacade) ?: return false
        return call.candidateDescriptor.isMutatorCall()
    }
}