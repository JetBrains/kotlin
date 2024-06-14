package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.utils.addIfNotNull

context(KaSession, KtObjCExportSession)
internal fun KaCallableSymbol.translateToObjCExportStub(): List<ObjCExportStub> {
    val result = mutableListOf<ObjCExportStub>()
    when (this) {
        is KaPropertySymbol -> {
            if (isObjCProperty) {
                result.addIfNotNull(translateToObjCProperty())
            } else {
                result.addIfNotNull(this.getter?.translateToObjCMethod())
                result.addIfNotNull(this.setter?.translateToObjCMethod())
            }
        }
        is KaFunctionSymbol -> result.addIfNotNull(translateToObjCMethod())
        else -> Unit
    }
    return result
}

context(KaSession, KtObjCExportSession)
internal fun KaClassOrObjectSymbol.translateToObjCExportStub(): ObjCClass? = when (classKind) {
    KaClassKind.INTERFACE -> translateToObjCProtocol()
    KaClassKind.CLASS -> translateToObjCClass()
    KaClassKind.OBJECT -> translateToObjCObject()
    KaClassKind.ENUM_CLASS -> translateToObjCClass()
    KaClassKind.COMPANION_OBJECT -> translateToObjCObject()
    else -> null
}