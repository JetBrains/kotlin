/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses

import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.sir.SirAvailability
import org.jetbrains.kotlin.sir.providers.SirDeclarationProvider
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTranslationResult
import org.jetbrains.kotlin.sir.providers.sirAvailability
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.sir.lightclasses.nodes.SirStubClassFromKtSymbol
import org.jetbrains.sir.lightclasses.nodes.SirStubProtocol

public class StubbingSirDeclarationProvider(
    private val sirSession: SirSession,
    private val declarationsProvider: SirDeclarationProvider,
) : SirDeclarationProvider {
    override fun KaDeclarationSymbol.toSir(): SirTranslationResult = sirSession.withSessions {
        when (sirAvailability()) {
            is SirAvailability.Available -> with(declarationsProvider) { toSir() }
            is SirAvailability.Hidden -> when (this@toSir) {
                is KaNamedClassSymbol if classKind == KaClassKind.INTERFACE -> SirTranslationResult.StubInterface(
                    SirStubProtocol(
                        ktSymbol = this@toSir,
                        sirSession = sirSession
                    )
                )
                is KaNamedClassSymbol -> SirTranslationResult.StubClass(
                    SirStubClassFromKtSymbol(
                        ktSymbol = this@toSir,
                        sirSession = sirSession
                    )
                )
                else -> SirTranslationResult.Untranslatable(KotlinSource(this@toSir))
            }
            is SirAvailability.Unavailable -> SirTranslationResult.Untranslatable(KotlinSource(this@toSir))
        }
    }
}