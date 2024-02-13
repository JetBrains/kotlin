package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol


internal val KtClassOrObjectSymbol.isCompanion: Boolean
    get() = classKind == KtClassKind.COMPANION_OBJECT