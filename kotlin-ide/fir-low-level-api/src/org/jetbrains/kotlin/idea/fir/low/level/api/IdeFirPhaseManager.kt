/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirPhaseManager
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirSessionInvalidator

@ThreadSafeMutableState
internal class IdeFirPhaseManager(
    private val lazyDeclarationResolver: FirLazyDeclarationResolver,
    private val cache: ModuleFileCache,
    private val sessionInvalidator: FirSessionInvalidator,
) : FirPhaseManager() {
    override fun ensureResolved(
        symbol: AbstractFirBasedSymbol<*>,
        requiredPhase: FirResolvePhase
    ) {
        val result = symbol.fir as FirDeclaration
        val availablePhase = result.resolvePhase
        if (availablePhase >= requiredPhase) return
        // NB: we should use session from symbol here, not transformer session (important for IDE)
        val provider = result.session.firProvider

        require(provider.isPhasedFirAllowed) {
            "Incorrect resolvePhase: actual: $availablePhase, expected: $requiredPhase\n For: ${symbol.fir.render()}"
        }

        try {
            lazyDeclarationResolver.lazyResolveDeclaration(result, cache, requiredPhase, checkPCE = true)
        } catch (e: Throwable) {
            sessionInvalidator.invalidate((symbol.fir as FirDeclaration).session)
            throw e
        }
    }
}
