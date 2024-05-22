/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility

public interface UnsupportedDeclarationReporter {
    public fun report(symbol: KtSymbolWithVisibility, reason: String)
}

public class SimpleUnsupportedDeclarationReporter : UnsupportedDeclarationReporter {

    private val _messages: MutableList<String> = mutableListOf()

    public val messages: List<String>
        get() = _messages.toList()

    override fun report(symbol: KtSymbolWithVisibility, reason: String) {
        val declarationName = when (symbol) {
            is KtCallableSymbol -> symbol.callableIdIfNonLocal?.asSingleFqName()?.asString()
            is KtClassOrObjectSymbol -> symbol.classIdIfNonLocal?.asSingleFqName()?.asString()
            is KtNamedSymbol -> symbol.name.asString()
            else -> null
        } ?: "declaration"
        _messages += "Can't export $declarationName: $reason"
    }
}

public object SilentUnsupportedDeclarationReporter : UnsupportedDeclarationReporter {
    override fun report(symbol: KtSymbolWithVisibility, reason: String) {}
}
