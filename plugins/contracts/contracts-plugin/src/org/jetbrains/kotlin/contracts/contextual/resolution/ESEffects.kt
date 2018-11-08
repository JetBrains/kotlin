/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.resolution

import org.jetbrains.kotlin.contracts.contextual.model.CleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.VerifierDeclaration
import org.jetbrains.kotlin.contracts.model.ESEffect
import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.contracts.model.ExtensionEffect

class ContextProviderEffect(
    val providerDeclaration: ProviderDeclaration,
    val references: List<ESValue?>,
    val owner: ESValue
) : ExtensionEffect() {
    override fun isImplies(other: ESEffect): Boolean? = null
}

class ContextVerifierEffect(
    val verifierDeclaration: VerifierDeclaration,
    val references: List<ESValue?>,
    val owner: ESValue
) : ExtensionEffect() {
    override fun isImplies(other: ESEffect): Boolean? = null
}

class ContextCleanerEffect(
    val cleanerDeclaration: CleanerDeclaration,
    val references: List<ESValue?>,
    val owner: ESValue
) : ExtensionEffect() {
    override fun isImplies(other: ESEffect): Boolean? = null
}