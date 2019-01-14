/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.transactions

import org.jetbrains.kotlin.contracts.contextual.model.*
import org.jetbrains.kotlin.contracts.contextual.util.ThisInstanceHolder
import org.jetbrains.kotlin.contracts.contextual.util.ValueInstanceHolder
import org.jetbrains.kotlin.contracts.description.expressions.ContractDescriptionValue
import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.contracts.model.structure.ESVariable
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext

internal class TransactionProviderDeclaration(override val references: List<ContractDescriptionValue>) : ProviderDeclaration {
    override val family = TransactionFamily

    override fun bind(sourceElement: KtElement, references: List<ESValue?>, bindingContext: BindingContext): ContextProvider? {
        val descriptor = extractThisVariableDescriptor(references) ?: return null
        return TransactionProvider(descriptor, sourceElement)
    }

    override fun toString(): String = "opened transaction"
}

sealed class TransactionVerifierDeclaration : VerifierDeclaration

internal class ClosedTransactionVerifierDeclaration(override val references: List<ContractDescriptionValue>) :
    TransactionVerifierDeclaration() {
    override val family = TransactionFamily

    override fun bind(sourceElement: KtElement, references: List<ESValue?>, bindingContext: BindingContext): ContextVerifier? {
        val instanceHolder = extractThisVariableDescriptor(references) ?: return null
        return ClosedTransactionVerifier(instanceHolder, sourceElement)
    }

    override fun toString(): String = "no opened transaction"
}

internal class OpenedTransactionVerifierDeclaration(override val references: List<ContractDescriptionValue>) :
    TransactionVerifierDeclaration() {
    override val family = TransactionFamily

    override fun bind(sourceElement: KtElement, references: List<ESValue?>, bindingContext: BindingContext): ContextVerifier? {
        val instanceHolder = extractThisVariableDescriptor(references) ?: return null
        return OpenedTransactionVerifier(instanceHolder, sourceElement)
    }

    override fun toString(): String = "opened transaction"
}

internal class TransactionCleanerDeclaration(override val references: List<ContractDescriptionValue>) : CleanerDeclaration {
    override val family = TransactionFamily

    override fun bind(sourceElement: KtElement, references: List<ESValue?>, bindingContext: BindingContext): ContextCleaner? {
        val instanceHolder = extractThisVariableDescriptor(references) ?: return null
        return TransactionCleaner(instanceHolder)
    }

    override fun toString(): String = "opened transaction"
}

private fun extractThisVariableDescriptor(references: List<ESValue?>): ThisInstanceHolder? {
    val thisReference = references.firstOrNull() as? ESVariable ?: return null
    return ValueInstanceHolder(thisReference.descriptor)
}