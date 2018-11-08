/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.resolution

import org.jetbrains.kotlin.contracts.ESLambda
import org.jetbrains.kotlin.contracts.contextual.FactsBindingInfo
import org.jetbrains.kotlin.contracts.model.ExtensionEffect
import org.jetbrains.kotlin.contracts.model.structure.ESFunction
import org.jetbrains.kotlin.extensions.ContractsInfoForInvocation
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

fun resolveContextualContracts(
    effect: ExtensionEffect,
    resolvedCall: ResolvedCall<*>,
    bindingContext: BindingContext
): ContractsInfoForInvocation? = when (effect) {
    is ContextProviderEffect -> extractProvider(
        effect,
        resolvedCall,
        bindingContext
    )
    is ContextVerifierEffect -> extractVerifier(
        effect,
        resolvedCall,
        bindingContext
    )
    is ContextCleanerEffect -> extractCleaner(
        effect,
        resolvedCall,
        bindingContext
    )
    else -> null
}

private fun extractProvider(
    effect: ContextProviderEffect,
    resolvedCall: ResolvedCall<*>,
    bindingContext: BindingContext
): ContractsInfoForInvocation? {
    val providerDeclaration = effect.providerDeclaration
    return when (effect.owner) {
        is ESFunction -> {
            val callExpression = resolvedCall.call.callElement as? KtCallExpression ?: return null
            val provider = providerDeclaration.bind(callExpression, effect.references, bindingContext) ?: return null
            ContractsInfoForInvocation(callExpression, FactsBindingInfo(provider))
        }
        is ESLambda -> {
            val lambda = effect.owner.lambda.functionLiteral
            val provider = providerDeclaration.bind(lambda, effect.references, bindingContext) ?: return null
            ContractsInfoForInvocation(lambda, FactsBindingInfo(provider))
        }
        else -> null
    }
}

private fun extractVerifier(
    effect: ContextVerifierEffect,
    resolvedCall: ResolvedCall<*>,
    bindingContext: BindingContext
): ContractsInfoForInvocation? {
    val verifierDeclaration = effect.verifierDeclaration
    return when (effect.owner) {
        is ESFunction -> {
            val callExpression = resolvedCall.call.callElement as? KtCallExpression ?: return null
            val verifier = verifierDeclaration.bind(callExpression, effect.references, bindingContext) ?: return null
            ContractsInfoForInvocation(callExpression, FactsBindingInfo(verifier))
        }
        is ESLambda -> {
            val lambda = effect.owner.lambda.functionLiteral
            val verifier = verifierDeclaration.bind(lambda, effect.references, bindingContext) ?: return null
            ContractsInfoForInvocation(lambda, FactsBindingInfo(verifier))
        }
        else -> null
    }
}

private fun extractCleaner(
    effect: ContextCleanerEffect,
    resolvedCall: ResolvedCall<*>,
    bindingContext: BindingContext
): ContractsInfoForInvocation? {
    val cleanerDeclaration = effect.cleanerDeclaration
    return when (effect.owner) {
        is ESFunction -> {
            val callExpression = resolvedCall.call.callElement as? KtCallExpression ?: return null
            val cleaner = cleanerDeclaration.bind(callExpression, effect.references, bindingContext) ?: return null
            ContractsInfoForInvocation(callExpression, FactsBindingInfo(cleaner))
        }
        is ESLambda -> {
            val lambda = effect.owner.lambda.functionLiteral
            val cleaner = cleanerDeclaration.bind(lambda, effect.references, bindingContext) ?: return null
            ContractsInfoForInvocation(lambda, FactsBindingInfo(cleaner))
        }
        else -> null
    }
}
