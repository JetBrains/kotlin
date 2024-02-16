package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClass
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtCallableSymbol.translateToObjCExportStub(): ObjCExportStub? {
    return when (this) {
        is KtPropertySymbol -> translateToObjCProperty()
        is KtFunctionSymbol -> translateToObjCMethod()
        else -> null
    }
}

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtClassOrObjectSymbol.translateToObjCExportStub(): ObjCClass? = when (classKind) {
    KtClassKind.INTERFACE -> translateToObjCProtocol()
    KtClassKind.CLASS -> translateToObjCClass()
    KtClassKind.OBJECT -> translateToObjCObject()
    KtClassKind.ENUM_CLASS -> translateToObjCClass()
    KtClassKind.COMPANION_OBJECT -> translateToObjCObject()
    else -> null
}