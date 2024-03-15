package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol

internal val KtFunctionLikeSymbol.isSuspend: Boolean
    get() = if (this is KtFunctionSymbol) isSuspend else false