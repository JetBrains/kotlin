package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId

internal val KtClassOrObjectSymbol.isCloneable: Boolean
    get() {
        return classIdIfNonLocal?.isCloneable ?: false
    }

internal val ClassId.isCloneable: Boolean
    get() {
        return asSingleFqName() == StandardNames.FqNames.cloneable.toSafe()
    }