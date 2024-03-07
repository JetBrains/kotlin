/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.kt.nodes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.sir.SirNamedDeclaration
import org.jetbrains.kotlin.sir.SirNominalType
import org.jetbrains.kotlin.sir.kt.SirSession


/**
 * A type which is derived from the given [KtClassOrObjectSymbol].
 */
public class SirNominalTypeFromSymbol(
    ktAnalysisSession: KtAnalysisSession,
    sirSession: SirSession,
    ktClassOrObjectSymbol: KtClassOrObjectSymbol,
) : SirNominalType {
    override val type: SirNamedDeclaration by lazy {
        with(sirSession) {
            ktClassOrObjectSymbol.sirDeclaration() as SirNamedDeclaration
        }
    }
}
