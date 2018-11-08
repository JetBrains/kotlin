/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.parsing

import org.jetbrains.kotlin.contracts.contextual.ContextualEffectSystem
import org.jetbrains.kotlin.contracts.contextual.resolution.*
import org.jetbrains.kotlin.contracts.description.EffectDeclaration
import org.jetbrains.kotlin.contracts.description.expressions.FunctionReferenceImpl
import org.jetbrains.kotlin.contracts.parsing.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

// requires / provides without block: KFunction<*> parsing
internal class PsiFactParser(
    collector: ContractParsingDiagnosticsCollector,
    callContext: ContractCallContext,
    dispatcher: PsiContractParserDispatcher
) : AbstractPsiEffectParser(collector, callContext, dispatcher) {
    override fun tryParseEffect(expression: KtExpression): Collection<EffectDeclaration> {
        if (expression !is KtCallExpression) return emptyList()

        val bindingContext = callContext.bindingContext

        val resolvedCall = expression.getResolvedCall(bindingContext) ?: return emptyList()
        val descriptor = resolvedCall.resultingDescriptor

        val argumentExpression = resolvedCall.firstArgumentAsExpressionOrNull() ?: return emptyList()
        val ownerFunction = expression.parents.firstOrNull { it is KtNamedFunction } as? KtNamedFunction ?: return emptyList()
        val ownerDescriptor = bindingContext[BindingContext.FUNCTION, ownerFunction] ?: return emptyList()
        val owner = FunctionReferenceImpl(ownerDescriptor)

        val descriptorName = descriptor.name

        if (!descriptor.isProviderOrVerifierOrCleanerDescriptor()) return emptyList()

        val (provider, verifier, cleaner) =
                extractDeclarations(
                    argumentExpression,
                    descriptorName,
                    bindingContext,
                    contractParserDispatcher
                ) ?: return emptyList()

        val declarations = mutableListOf<EffectDeclaration?>()
        declarations += provider?.let {
            ContextProviderEffectDeclaration(
                it,
                it.references,
                owner
            )
        }
        declarations += verifier?.let {
            ContextVerifierEffectDeclaration(
                it,
                it.references,
                owner
            )
        }
        declarations += cleaner?.let {
            ContextCleanerEffectDeclaration(
                it,
                it.references,
                owner
            )
        }

        return declarations.filterNotNull()
    }
}

// requires / provides with block: KFunction<*> parsing
internal class PsiLambdaFactParser(
    collector: ContractParsingDiagnosticsCollector,
    callContext: ContractCallContext,
    dispatcher: PsiContractParserDispatcher
) : AbstractPsiEffectParser(collector, callContext, dispatcher) {
    override fun tryParseEffect(expression: KtExpression): Collection<EffectDeclaration> {
        if (expression !is KtCallExpression) return emptyList()

        val bindingContext = callContext.bindingContext

        val resolvedCall = expression.getResolvedCall(bindingContext) ?: return emptyList()
        val descriptor = resolvedCall.resultingDescriptor

        val ownerExpression = resolvedCall.argumentAsExpressionOrNull(0) ?: return emptyList()
        val owner = contractParserDispatcher.parseVariable(ownerExpression) ?: return emptyList()

        val argumentExpression = resolvedCall.argumentAsExpressionOrNull(1) ?: return emptyList()

        val descriptorName = descriptor.name

        if (!descriptor.isProviderOrVerifierOrCleanerDescriptor()) return emptyList()

        val (provider, verifier, cleaner) =
                extractDeclarations(
                    argumentExpression,
                    descriptorName,
                    bindingContext,
                    contractParserDispatcher
                ) ?: return emptyList()

        val declarations = mutableListOf<EffectDeclaration?>()
        declarations += provider?.let {
            LambdaContextProviderEffectDeclaration(
                it,
                it.references,
                owner
            )
        }
        declarations += verifier?.let {
            LambdaContextVerifierEffectDeclaration(
                it,
                it.references,
                owner
            )
        }
        declarations += cleaner?.let {
            LambdaContextCleanerEffectDeclaration(
                it,
                it.references,
                owner
            )
        }

        return declarations.filterNotNull()
    }
}


private fun extractDeclarations(
    expression: KtExpression,
    name: Name,
    bindingContext: BindingContext,
    dispatcher: PsiContractParserDispatcher
): ContextDeclarations? =
    ContextualEffectSystem.getParsers(expression.project, bindingContext, dispatcher)
        .asSequence()
        .map { it.extractDeclarationsOrNull(expression, name) }
        .filterNotNull()
        .firstOrNull()