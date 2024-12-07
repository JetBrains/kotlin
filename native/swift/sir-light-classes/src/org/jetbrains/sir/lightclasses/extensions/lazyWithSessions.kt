/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.extensions

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.sir.lightclasses.SirFromKtSymbol

internal interface SirAndKaSession : KaSession, SirSession

internal class SirAndKaSessionImpl(
    override val sirSession: SirSession,
    private val kaSession: KaSession
) : SirAndKaSession, KaSession by kaSession, SirSession by sirSession

internal inline fun <reified S : KaDeclarationSymbol, reified R> SirFromKtSymbol<S>.lazyWithSessions(
    crossinline block: SirAndKaSession.() -> R
): Lazy<R> {
    return lazy {
        withSessions(block)
    }
}

internal inline fun <reified S : KaDeclarationSymbol, reified R> SirFromKtSymbol<S>.withSessions(
    crossinline block: SirAndKaSession.() -> R,
): R {
    return analyze(ktModule) {
        val sirAndKaSession = SirAndKaSessionImpl(sirSession, useSiteSession)
        sirAndKaSession.block()
    }
}
