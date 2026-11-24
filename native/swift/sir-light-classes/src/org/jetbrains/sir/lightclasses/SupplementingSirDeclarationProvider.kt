/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.providers.SirDeclarationProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTranslationResult
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.sir.lightclasses.supplements.SirArrayToListExtensionProvider

public class SupplementingSirDeclarationProvider(
    private val sirSession: SirSession,
    private val declarationsProvider: SirDeclarationProvider,
    private val providers: List<SirSupplementaryDeclarationsProvider> = listOf(
        SirArrayToListExtensionProvider
    )
) : SirDeclarationProvider {

    override fun KaDeclarationSymbol.toSir(): SirTranslationResult = sirSession.withSessions {
        val result = with(declarationsProvider) { toSir() }
        if (result !is Supplementable) return@withSessions result
        val declaration = result.primaryDeclaration ?: return@withSessions result
        val declarations = providers.flatMap { it.provide(declaration) }
        if (declarations.isEmpty()) result else result.supplement(declarations)
    }
}

public interface SirSupplementaryDeclarationsProvider {
    context(sirSession: SirSession, kaSession: KaSession)
    public fun provide(declaration: SirDeclaration): List<SirDeclaration>
}
