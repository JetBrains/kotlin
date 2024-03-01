package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClassType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.swiftNameAttribute
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId

context(KtAnalysisSession)
internal val KtClassOrObjectSymbol.isThrowable: Boolean
    get() {
        val classId = classIdIfNonLocal ?: return false
        return classId.isThrowable
    }

context(KtAnalysisSession)
internal val ClassId.isThrowable: Boolean
    get() {
        return StandardNames.FqNames.throwable == this.asSingleFqName()
    }

internal fun buildThrowableAsErrorMethod(): ObjCMethod {
    return ObjCMethod(
        comment = null,
        isInstanceMethod = true,
        returnType = ObjCClassType("NSError"),
        selectors = listOf("asError"),
        parameters = emptyList(),
        attributes = listOf(swiftNameAttribute("asError()")),
        origin = null
    )
}