package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol

internal val KaFunctionLikeSymbol.isSuspend: Boolean
    get() = if (this is KaFunctionSymbol) isSuspend else false