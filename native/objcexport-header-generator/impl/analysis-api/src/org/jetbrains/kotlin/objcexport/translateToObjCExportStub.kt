package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.utils.addIfNotNull

context(KtAnalysisSession, KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KtCallableSymbol.translateToObjCExportStub(): List<ObjCExportStub> {
    val result = mutableListOf<ObjCExportStub>()
    when (this) {
        is KtPropertySymbol -> {
            if (isObjCProperty) {
                result.addIfNotNull(translateToObjCProperty())
            } else {
                result.addIfNotNull(this.getter?.translateToObjCMethod())
                result.addIfNotNull(this.setter?.translateToObjCMethod())
            }
        }
        is KtFunctionSymbol -> result.addIfNotNull(translateToObjCMethod())
        else -> Unit
    }
    return result
}

context(KtAnalysisSession, KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun KtClassOrObjectSymbol.translateToObjCExportStub(): ObjCClass? = when (classKind) {
    KtClassKind.INTERFACE -> translateToObjCProtocol()
    KtClassKind.CLASS -> translateToObjCClass()
    KtClassKind.OBJECT -> translateToObjCObject()
    KtClassKind.ENUM_CLASS -> translateToObjCClass()
    KtClassKind.COMPANION_OBJECT -> translateToObjCObject()
    else -> null
}