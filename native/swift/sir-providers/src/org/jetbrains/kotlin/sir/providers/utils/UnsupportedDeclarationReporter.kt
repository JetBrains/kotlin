/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.utils

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.name

public interface UnsupportedDeclarationReporter {
    public val messages: List<String>
    public fun report(symbol: KaDeclarationSymbol, reason: String)
}

public class SimpleUnsupportedDeclarationReporter : UnsupportedDeclarationReporter {

    private val _messages: MutableList<String> = mutableListOf()

    public override val messages: List<String>
        get() = _messages.toList()

    override fun report(symbol: KaDeclarationSymbol, reason: String) {
        val declarationName = when (symbol) {
            is KaCallableSymbol -> symbol.callableId?.asSingleFqName()?.asString()
            is KaClassSymbol -> symbol.classId?.asSingleFqName()?.asString()
            else -> symbol.name?.asString()
        } ?: "declaration"
        _messages += "Can't export $declarationName: $reason"
    }
}

public object SilentUnsupportedDeclarationReporter : UnsupportedDeclarationReporter {
    public override val messages: List<String> = emptyList()
    override fun report(symbol: KaDeclarationSymbol, reason: String) {}
}
