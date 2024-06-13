package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId

context(KtAnalysisSession)
internal val KtClassOrObjectSymbol.isThrowable: Boolean
    get() {
        val classId = classId ?: return false
        return classId.isThrowable
    }

context(KtAnalysisSession)
internal val ClassId.isThrowable: Boolean
    get() {
        return StandardNames.FqNames.throwable == this.asSingleFqName()
    }