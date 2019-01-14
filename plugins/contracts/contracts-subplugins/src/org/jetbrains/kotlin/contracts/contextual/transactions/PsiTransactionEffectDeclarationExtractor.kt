/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.transactions

import org.jetbrains.kotlin.contracts.contextual.parsing.ContextDeclarations
import org.jetbrains.kotlin.contracts.contextual.parsing.ContextDslNames
import org.jetbrains.kotlin.contracts.contextual.parsing.PsiEffectDeclarationExtractor
import org.jetbrains.kotlin.contracts.description.expressions.VariableReference
import org.jetbrains.kotlin.contracts.parsing.PsiContractVariableParserDispatcher
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext

internal class PsiTransactionEffectDeclarationExtractor(
    context: BindingContext,
    dispatcher: PsiContractVariableParserDispatcher
) : PsiEffectDeclarationExtractor(context, dispatcher) {
    companion object {
        private const val CONSTRUCTOR_NAME = "OpenedTransaction"
    }

    override fun extractDeclarations(declaration: KtExpression, dslFunctionName: Name): ContextDeclarations {
        if (dslFunctionName !in setOf(ContextDslNames.STARTS, ContextDslNames.REQUIRES, ContextDslNames.CLOSES))
            return ContextDeclarations()

        val thisReference = extractThisReference(declaration) ?: return ContextDeclarations()
        val references = listOf(thisReference)

        return when (dslFunctionName) {
            ContextDslNames.STARTS -> {
                val provider = TransactionProviderDeclaration(references)
                val verifier = ClosedTransactionVerifierDeclaration(references)
                ContextDeclarations(provider = provider, verifier = verifier)
            }
            ContextDslNames.REQUIRES -> {
                val verifier = OpenedTransactionVerifierDeclaration(references)
                ContextDeclarations(verifier = verifier)
            }
            ContextDslNames.CLOSES -> {
                val verifier = OpenedTransactionVerifierDeclaration(references)
                val cleaner = TransactionCleanerDeclaration(references)
                ContextDeclarations(verifier = verifier, cleaner = cleaner)
            }
            else -> ContextDeclarations()
        }
    }

    private fun extractThisReference(declaration: KtExpression): VariableReference? {
        if (declaration !is KtCallExpression) return null
        val (resolvedCall, descriptor) = declaration.getResolverCallAndResultingDescriptor() ?: return null

        if (descriptor.extractConstructorName() != CONSTRUCTOR_NAME) return null

        return dispatcher.parseVariable(resolvedCall.firstArgumentAsExpressionOrNull())
    }
}