/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.dslmarker

import org.jetbrains.kotlin.contracts.contextual.parsing.ContextDeclarations
import org.jetbrains.kotlin.contracts.contextual.parsing.ContextDslNames
import org.jetbrains.kotlin.contracts.contextual.parsing.PsiEffectDeclarationExtractor
import org.jetbrains.kotlin.contracts.parsing.PsiContractVariableParserDispatcher
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getFirstArgumentExpression

internal class PsiDslMarkerEffectDeclarationExtractor(
    context: BindingContext,
    dispatcher: PsiContractVariableParserDispatcher
) : PsiEffectDeclarationExtractor(context, dispatcher) {
    companion object {
        private const val CONSTRUCTOR_NAME = "DslMarkers"
    }

    override fun extractDeclarations(declaration: KtExpression, dslFunctionName: Name): ContextDeclarations {
        val (resolvedCall, descriptor) = declaration.getResolverCallAndResultingDescriptor() ?: return ContextDeclarations()

        val constructorName = descriptor.extractConstructorName() ?: return ContextDeclarations()
        if (constructorName != CONSTRUCTOR_NAME) return ContextDeclarations()

        return when (dslFunctionName) {
            ContextDslNames.CALLS_IN -> {
                val receiverReference =
                    dispatcher.parseLambdaReceiver(resolvedCall.getFirstArgumentExpression()) ?: return ContextDeclarations()
                ContextDeclarations(
                    provider = DslMarkerProviderDeclaration(
                        listOf(receiverReference)
                    )
                )
            }
            ContextDslNames.REQUIRES -> {
                val thisReference = dispatcher.parseVariable(resolvedCall.getFirstArgumentExpression()) ?: return ContextDeclarations()
                ContextDeclarations(
                    verifier = DslMarkerVerifierDeclaration(
                        listOf(thisReference)
                    )
                )
            }
            else -> ContextDeclarations()
        }
    }
}