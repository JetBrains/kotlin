package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId

context(KaSession)
internal val KaClassOrObjectSymbol.isThrowable: Boolean
    get() {
        val classId = classId ?: return false
        return classId.isThrowable
    }

context(KaSession)
internal val ClassId.isThrowable: Boolean
    get() {
        return StandardNames.FqNames.throwable == this.asSingleFqName()
    }