/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.parsing

import org.jetbrains.kotlin.contracts.parsing.equalsDslDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.Name

object ContractsDslNames {
    val PROVIDES_CONTEXT = Name.identifier("ProvidesContext")
    val CALLS_BLOCK_IN_CONTEXT = Name.identifier("CallsBlockInContext")
    val REQUIRES_CONTEXT = Name.identifier("RequiresContext")
    val BLOCK_EXPECTS_TO_CONTEXT = Name.identifier("BlockExpectsToContext")
    val BLOCK_NOT_EXPECTS_TO_CONTEXT = Name.identifier("RequiresNotContext")
    val BLOCK_REQUIRES_NOT_CONTEXT = Name.identifier("BlockNotExpectsToContext")
    val STARTS_CONTEXT = Name.identifier("StartsContext")
    val CLOSES_CONTEXT = Name.identifier("ClosesContext")

    // Effect-declaration calls
    val RETURNS = Name.identifier("returns")
    val RETURNS_NOT_NULL = Name.identifier("returnsNotNull")
    val CALLS_IN_PLACE = Name.identifier("callsInPlace")
    val PROVIDES = Name.identifier("provides")
    val REQUIRES = Name.identifier("requires")
    val REQUIRES_NOT = Name.identifier("requiresNot")
    val STARTS = Name.identifier("starts")
    val CLOSES = Name.identifier("closes")
    val CALLS_IN = Name.identifier("callsIn")
    val EXPECTS_TO = Name.identifier("expectsTo")
    val NOT_EXPECTS_TO = Name.identifier("notExpectsTo")
}

object ContextDslNames {
    val PROVIDES = ContractsDslNames.PROVIDES
    val REQUIRES = ContractsDslNames.REQUIRES
    val REQUIRES_NOT = ContractsDslNames.REQUIRES_NOT
    val STARTS = ContractsDslNames.STARTS
    val CLOSES = ContractsDslNames.CLOSES
    val CALLS_IN = ContractsDslNames.CALLS_IN
    val EXPECTS_TO = ContractsDslNames.EXPECTS_TO
    val NOT_EXPECTS_TO = ContractsDslNames.NOT_EXPECTS_TO
}

fun DeclarationDescriptor.isProviderOrVerifierOrCleanerDescriptor(): Boolean =
    isProvidesFactDescriptor() ||
            isStartsContextDescriptor() ||
            isRequiresContextDescriptor() ||
            isRequiresNotContextDescriptor() ||
            isClosesContextDescriptor() ||
            isCallsInDescriptor() ||
            isExpectsToDescriptor() ||
            isNotExpectsToDescriptor()

private fun DeclarationDescriptor.isProvidesFactDescriptor(): Boolean = equalsDslDescriptor(ContractsDslNames.PROVIDES)
private fun DeclarationDescriptor.isStartsContextDescriptor(): Boolean = equalsDslDescriptor(ContractsDslNames.STARTS)
private fun DeclarationDescriptor.isRequiresContextDescriptor(): Boolean = equalsDslDescriptor(ContractsDslNames.REQUIRES)
private fun DeclarationDescriptor.isRequiresNotContextDescriptor(): Boolean = equalsDslDescriptor(ContractsDslNames.REQUIRES_NOT)
private fun DeclarationDescriptor.isClosesContextDescriptor(): Boolean = equalsDslDescriptor(ContractsDslNames.CLOSES)
private fun DeclarationDescriptor.isCallsInDescriptor(): Boolean = equalsDslDescriptor(ContractsDslNames.CALLS_IN)
private fun DeclarationDescriptor.isExpectsToDescriptor(): Boolean = equalsDslDescriptor(ContractsDslNames.EXPECTS_TO)
private fun DeclarationDescriptor.isNotExpectsToDescriptor(): Boolean = equalsDslDescriptor(ContractsDslNames.NOT_EXPECTS_TO)