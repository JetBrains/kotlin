/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.impl

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.providers.SirChildrenProvider
import org.jetbrains.kotlin.sir.providers.SirSession

public class SirDeclarationChildrenProviderImpl(private val sirSession: SirSession) : SirChildrenProvider {
    override fun Sequence<KaDeclarationSymbol>.extractDeclarations(kaSession: KaSession): Sequence<SirDeclaration> = with(sirSession) {
        filter { isAccessible(it, kaSession) }
            .flatMap { it.toSir().allDeclarations }
            .flatMap { listOf(it) + it.trampolineDeclarations() }
    }

    private fun SirSession.isAccessible(symbol: KaDeclarationSymbol, kaSession: KaSession): Boolean =
        when (symbol.sirVisibility(kaSession)) {
            null, SirVisibility.PRIVATE, SirVisibility.FILEPRIVATE, SirVisibility.INTERNAL -> false
            SirVisibility.PUBLIC, SirVisibility.PACKAGE -> true
        }
}
