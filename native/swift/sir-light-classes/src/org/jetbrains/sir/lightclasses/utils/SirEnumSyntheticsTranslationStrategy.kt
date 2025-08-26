/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.sir.lightclasses.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.sir.SirEnum
import org.jetbrains.kotlin.sir.providers.SirSession
import org.jetbrains.kotlin.sir.providers.SirTranslationResult
import org.jetbrains.kotlin.sir.providers.getSirParent
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.withSessions
import org.jetbrains.sir.lightclasses.nodes.SirFailableInitFromKtValueOfFunctionSymbol

public sealed class SirEnumSyntheticsTranslationStrategy(public val kaSymbol: KaNamedFunctionSymbol) {
    public companion object {
        public operator fun invoke(kaSymbol: KaSymbol): SirEnumSyntheticsTranslationStrategy? =
            (kaSymbol as? KaNamedFunctionSymbol)
                ?.let { symbol ->
                    when (symbol.name.identifier) {
                        "valueOf" if symbol.isStatic -> AsFailableInit(kaSymbol)
                        else -> null
                    }
                }
    }

    public open fun translate(sirSession: SirSession): SirTranslationResult = SirTranslationResult.Untranslatable(KotlinSource(kaSymbol))

    public class AsFailableInit(kaSymbol: KaNamedFunctionSymbol) : SirEnumSyntheticsTranslationStrategy(kaSymbol) {
        override fun translate(sirSession: SirSession): SirTranslationResult {
            return SirTranslationResult.Constructor(
                SirFailableInitFromKtValueOfFunctionSymbol(
                    kaSymbol, sirSession.withSessions { kaSymbol.getSirParent() as SirEnum }, sirSession
                )
            )
        }
    }
}
