/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.contextual.cfg

import org.jetbrains.kotlin.contracts.contextual.model.CleanerDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.ProviderDeclaration
import org.jetbrains.kotlin.contracts.contextual.model.VerifierDeclaration

data class ContextContracts internal constructor(
    val providers: Collection<ProviderDeclaration> = listOf(),
    val verifiers: Collection<VerifierDeclaration> = listOf(),
    val cleaners: Collection<CleanerDeclaration> = listOf()
)