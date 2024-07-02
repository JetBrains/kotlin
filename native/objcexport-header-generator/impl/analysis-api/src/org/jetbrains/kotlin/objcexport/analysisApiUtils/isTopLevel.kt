package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.objcexport.getClassIfCategory

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal val KaCallableSymbol.isTopLevel: Boolean
    get() = callableId?.classId == null && getClassIfCategory(this) == null
