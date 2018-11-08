/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.safebuilders

import org.jetbrains.kotlin.contracts.contextual.model.*
import org.jetbrains.kotlin.contracts.contextual.util.FunctionAndThisInstanceHolder
import org.jetbrains.kotlin.contracts.contextual.util.ThisInstanceHolder.Companion.fromESValue
import org.jetbrains.kotlin.contracts.description.InvocationKind
import org.jetbrains.kotlin.contracts.description.expressions.ContractDescriptionValue
import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.contracts.model.structure.ESFunction
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext

internal class CallProviderDeclaration(override val references: List<ContractDescriptionValue>) : ProviderDeclaration {
    override val family = CallFamily

    override fun bind(sourceElement: KtElement, references: List<ESValue?>, bindingContext: BindingContext): ContextProvider? {
        val holder = extractFunctionAndThisInstanceHolder(references) ?: return null
        return CallProvider(holder, sourceElement)
    }

    override fun toString(): String = "func called EXACTLY_ONCE"
}

internal class CallVerifierDeclaration(
    internal val kind: InvocationKind,
    override val references: List<ContractDescriptionValue>
) : VerifierDeclaration {
    override val family = CallFamily

    override fun bind(sourceElement: KtElement, references: List<ESValue?>, bindingContext: BindingContext): ContextVerifier? {
        val holder = extractFunctionAndThisInstanceHolder(references) ?: return null
        return CallVerifier(holder, kind, sourceElement)
    }

    override fun toString(): String = "func needs to be called $kind"
}

internal class CallCleanerDeclaration(
    internal val kind: InvocationKind,
    override val references: List<ContractDescriptionValue>
) : CleanerDeclaration {
    override val family = CallFamily

    override fun bind(sourceElement: KtElement, references: List<ESValue?>, bindingContext: BindingContext): ContextCleaner? {
        val holder = extractFunctionAndThisInstanceHolder(references) ?: return null
        return CallCleaner(holder)
    }

    override fun toString(): String = "func needs to be called $kind"
}

private fun extractFunctionAndThisInstanceHolder(references: List<ESValue?>): FunctionAndThisInstanceHolder? {
    val functionDescriptor = (references[0] as? ESFunction)?.descriptor ?: return null
    val thisHolder = fromESValue(references[1]) ?: return null
    return FunctionAndThisInstanceHolder(functionDescriptor, thisHolder)
}