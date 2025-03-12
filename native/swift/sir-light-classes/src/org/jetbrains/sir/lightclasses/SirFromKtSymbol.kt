/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses

import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.source.KotlinSource

internal interface SirFromKtSymbol<S : KaDeclarationSymbol> {
    val ktSymbol: S
    val sirSession: SirSession
}

/**
 * Interface indicating that supporting type is a subject to type binding generation
 *
 * @see TypeBindingBridgeRequest
 * @see TypeBindingBridge
 */
public interface BindableBridgedType {
    public val origin: KotlinSource
}
