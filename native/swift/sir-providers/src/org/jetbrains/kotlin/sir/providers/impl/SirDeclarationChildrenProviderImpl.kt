/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.sir.SirAvailability
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirChildrenProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.sirAvailability
import org.jetbrains.kotlin.sir.providers.toSir
import org.jetbrains.kotlin.sir.providers.trampolineDeclarations
import org.jetbrains.kotlin.sir.providers.withSessions

public class SirDeclarationChildrenProviderImpl(private val sirSession: SirSession) : SirChildrenProvider {
    override fun Sequence<KaDeclarationSymbol>.extractDeclarations(): Sequence<SirDeclaration> =
        sirSession.withSessions {
            filter { isAccessible(it) }
                .flatMap { it.toSir().allDeclarations }
                .flatMap { decl -> listOf(decl) + decl.trampolineDeclarations().filter { it.parent == decl } }
        }

    context(sir: SirSession)
    private fun isAccessible(symbol: KaDeclarationSymbol): Boolean =
        when (val availability = symbol.sirAvailability()) {
            is SirAvailability.Available -> when (availability.visibility) {
                SirVisibility.PUBLIC, SirVisibility.PACKAGE -> true
                SirVisibility.PRIVATE, SirVisibility.FILEPRIVATE, SirVisibility.INTERNAL -> false
            }
            is SirAvailability.Unavailable -> false
            is SirAvailability.Hidden -> true // these will need to be stubbed at some later stage
        }
}