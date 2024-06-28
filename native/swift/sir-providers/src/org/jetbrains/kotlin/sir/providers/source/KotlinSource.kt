/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.providers.source

import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirParameter

public data class KotlinSource(
    val symbol: KaSymbol,
) : SirOrigin.Foreign.SourceCode

public class KotlinRuntimeElement : SirOrigin.Foreign.SourceCode

public sealed class KotlinParameterOrigin : SirParameter.Origin {
    public class ValueParameter(public val parameter: KaValueParameterSymbol) : KotlinParameterOrigin()
}