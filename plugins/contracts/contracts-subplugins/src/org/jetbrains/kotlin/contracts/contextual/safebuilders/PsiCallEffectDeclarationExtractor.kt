/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.safebuilders

import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.parsing.ContextDeclarations
import org.jetbrains.kotlin.contracts.contextual.parsing.ContextDslNames
import org.jetbrains.kotlin.contracts.contextual.parsing.PsiEffectDeclarationExtractor
import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.contracts.description.expressions.ContractDescriptionValue
import org.jetbrains.kotlin.contracts.parsing.PsiContractVariableParserDispatcher
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext

internal class PsiCallEffectDeclarationExtractor(context: BindingContext, dispatcher: PsiContractVariableParserDispatcher) :
    PsiEffectDeclarationExtractor(context, dispatcher) {
    companion object {
        private const val CALLS = "Calls"
        private const val CALL_KIND = "CallKind"
    }

    override fun extractDeclarations(declaration: KtExpression, dslFunctionName: Name): ContextDeclarations {
        return when (dslFunctionName) {
            ContextDslNames.PROVIDES -> {
                val provider = extractProviderDeclaration(declaration, dslFunctionName)
                ContextDeclarations(provider = provider)
            }
            ContextDslNames.EXPECTS_TO -> {
                val (kind, references) = extractKindAndReferences(declaration) ?: return ContextDeclarations()
                val verifier = CallVerifierDeclaration(kind, references)
                val cleaner = CallCleanerDeclaration(kind, references)
                ContextDeclarations(verifier = verifier, cleaner = cleaner)
            }
            else -> ContextDeclarations()
        }
    }

    private fun extractProviderDeclaration(declaration: KtExpression, dslFunctionName: Name): ProviderDeclaration? {
        if (declaration !is KtCallExpression) return null

        val (resolvedCall, descriptor) = declaration.getResolverCallAndResultingDescriptor() ?: return null

        val constructorName = descriptor.extractConstructorName() ?: return null
        if (constructorName != CALLS) return null

        val functionReference = dispatcher.parseFunction(resolvedCall.argumentAsExpressionOrNull(0)) ?: return null
        val thisReference = dispatcher.parseVariable(resolvedCall.argumentAsExpressionOrNull(1)) ?: return null

        val references = listOf(functionReference, thisReference)
        return CallProviderDeclaration(references)
    }

    private fun extractKindAndReferences(declaration: KtExpression): Pair<InvocationKind, List<ContractDescriptionValue>>? {
        if (declaration !is KtCallExpression) return null

        val (resolvedCall, descriptor) = declaration.getResolverCallAndResultingDescriptor() ?: return null

        val constructorName = descriptor.extractConstructorName() ?: return null
        if (constructorName != CALL_KIND) return null

        val functionReference = dispatcher.parseFunction(resolvedCall.argumentAsExpressionOrNull(0)) ?: return null
        val kind = dispatcher.parseKind(resolvedCall.argumentAsExpressionOrNull(1)) ?: return null
        val receiverReference = dispatcher.parseLambdaReceiver(resolvedCall.argumentAsExpressionOrNull(2)) ?: return null

        val references = listOf(functionReference, receiverReference)

        return kind to references
    }
}