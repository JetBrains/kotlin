/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.resolution

import org.jetbrains.kotlin.contracts.contextual.model.*
import org.jetbrains.kotlin.contracts.description.ExtensionEffectDeclaration
import org.jetbrains.kotlin.contracts.description.expressions.ContractDescriptionValue
import org.jetbrains.kotlin.contracts.description.expressions.FunctionReference
import org.jetbrains.kotlin.contracts.description.expressions.VariableReference

interface ContextualEffectDeclaration<T : ContextEntity, R : ContextEntityDeclaration<T>, O : ContractDescriptionValue> :
    ExtensionEffectDeclaration {
    val factory: R
    val references: List<ContractDescriptionValue>
    val owner: O
}

data class ContextProviderEffectDeclaration(
    override val factory: ProviderDeclaration,
    override val references: List<ContractDescriptionValue>,
    override val owner: FunctionReference
) : ContextualEffectDeclaration<ContextProvider, ProviderDeclaration, FunctionReference>

data class LambdaContextProviderEffectDeclaration(
    override val factory: ProviderDeclaration,
    override val references: List<ContractDescriptionValue>,
    override val owner: VariableReference
) : ContextualEffectDeclaration<ContextProvider, ProviderDeclaration, VariableReference>

data class ContextVerifierEffectDeclaration(
    override val factory: VerifierDeclaration,
    override val references: List<ContractDescriptionValue>,
    override val owner: FunctionReference
) : ContextualEffectDeclaration<ContextVerifier, VerifierDeclaration, FunctionReference>

data class LambdaContextVerifierEffectDeclaration(
    override val factory: VerifierDeclaration,
    override val references: List<ContractDescriptionValue>,
    override val owner: VariableReference
) : ContextualEffectDeclaration<ContextVerifier, VerifierDeclaration, VariableReference>

data class ContextCleanerEffectDeclaration(
    override val factory: CleanerDeclaration,
    override val references: List<ContractDescriptionValue>,
    override val owner: FunctionReference
) : ContextualEffectDeclaration<ContextCleaner, CleanerDeclaration, FunctionReference>

data class LambdaContextCleanerEffectDeclaration(
    override val factory: CleanerDeclaration,
    override val references: List<ContractDescriptionValue>,
    override val owner: VariableReference
) : ContextualEffectDeclaration<ContextCleaner, CleanerDeclaration, VariableReference>