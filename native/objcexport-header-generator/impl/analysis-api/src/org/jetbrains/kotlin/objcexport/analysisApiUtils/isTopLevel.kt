package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol

internal val KaCallableSymbol.isTopLevel: Boolean
    get() = callableId?.classId == null
