package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol

internal val KtCallableSymbol.isTopLevel: Boolean
    get() = callableId?.classId == null
