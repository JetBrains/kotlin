package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol


internal val KaClassOrObjectSymbol.isCompanion: Boolean
    get() = classKind == KaClassKind.COMPANION_OBJECT