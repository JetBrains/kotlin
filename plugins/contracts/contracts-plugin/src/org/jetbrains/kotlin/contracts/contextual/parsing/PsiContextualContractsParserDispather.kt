/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.parsing

import org.jetbrains.kotlin.contracts.description.EffectDeclaration
import org.jetbrains.kotlin.contracts.parsing.*

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getType

class PsiContextualContractsParserDispather(
    private val collector: ContractParsingDiagnosticsCollector,
    private val callContext: ContractCallContext,
    dispatcher: PsiContractParserDispatcher
) : ExtensionParserDispatcher {
    private val effectsParsers: Map<Name, PsiEffectParser> = mapOf(
        ContractsDslNames.PROVIDES_CONTEXT to PsiFactParser(
            collector,
            callContext,
            dispatcher
        ),
        ContractsDslNames.STARTS_CONTEXT to PsiFactParser(
            collector,
            callContext,
            dispatcher
        ),
        ContractsDslNames.CLOSES_CONTEXT to PsiFactParser(
            collector,
            callContext,
            dispatcher
        ),
        ContractsDslNames.REQUIRES_CONTEXT to PsiFactParser(
            collector,
            callContext,
            dispatcher
        ),
        ContractsDslNames.BLOCK_NOT_EXPECTS_TO_CONTEXT to PsiFactParser(
            collector,
            callContext,
            dispatcher
        ),
        ContractsDslNames.CALLS_BLOCK_IN_CONTEXT to PsiLambdaFactParser(
            collector,
            callContext,
            dispatcher
        ),
        ContractsDslNames.BLOCK_EXPECTS_TO_CONTEXT to PsiLambdaFactParser(
            collector,
            callContext,
            dispatcher
        ),
        ContractsDslNames.BLOCK_REQUIRES_NOT_CONTEXT to PsiLambdaFactParser(
            collector,
            callContext,
            dispatcher
        )
    )

    override fun parseEffects(expression: KtExpression): Collection<EffectDeclaration> {
        val returnType = expression.getType(callContext.bindingContext) ?: return emptyList()
        val parser = effectsParsers[returnType.constructor.declarationDescriptor?.name]
        if (parser == null) {
            collector.badDescription("unrecognized effect", expression)
            return emptyList()
        }

        return parser.tryParseEffect(expression)
    }
}