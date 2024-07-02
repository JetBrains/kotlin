package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.objcexport.getClassIfCategory

internal fun KaSession.isTopLevel(symbol: KaCallableSymbol): Boolean =
    symbol.callableId?.classId == null && getClassIfCategory(symbol) == null
