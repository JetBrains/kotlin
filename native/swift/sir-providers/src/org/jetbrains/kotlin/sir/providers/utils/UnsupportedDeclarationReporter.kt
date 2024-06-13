/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithVisibility

public interface UnsupportedDeclarationReporter {
    public fun report(symbol: KaSymbolWithVisibility, reason: String)
}

public class SimpleUnsupportedDeclarationReporter : UnsupportedDeclarationReporter {

    private val _messages: MutableList<String> = mutableListOf()

    public val messages: List<String>
        get() = _messages.toList()

    override fun report(symbol: KaSymbolWithVisibility, reason: String) {
        val declarationName = when (symbol) {
            is KaCallableSymbol -> symbol.callableId?.asSingleFqName()?.asString()
            is KaClassOrObjectSymbol -> symbol.classId?.asSingleFqName()?.asString()
            is KaNamedSymbol -> symbol.name.asString()
            else -> null
        } ?: "declaration"
        _messages += "Can't export $declarationName: $reason"
    }
}

public object SilentUnsupportedDeclarationReporter : UnsupportedDeclarationReporter {
    override fun report(symbol: KaSymbolWithVisibility, reason: String) {}
}
