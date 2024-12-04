/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.providers.SirDeclarationProvider

public class CachingSirDeclarationProvider(
    private val declarationsProvider: SirDeclarationProvider,
) : SirDeclarationProvider {

    private val visitedDeclarations: MutableMap<KaDeclarationSymbol, List<SirDeclaration>> = mutableMapOf()

    override fun KaDeclarationSymbol.sirDeclarations(): List<SirDeclaration> {
        return visitedDeclarations.getOrPut(this@sirDeclarations) {
            with(declarationsProvider) { this@sirDeclarations.sirDeclarations() }
        }
    }

}
