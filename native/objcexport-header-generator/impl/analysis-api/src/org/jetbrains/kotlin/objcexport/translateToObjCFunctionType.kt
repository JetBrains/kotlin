package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.objcexport.*

internal fun ObjCExportContext.translateToObjCFunctionType(type: KaType, typeBridge: BlockPointerBridge): ObjCReferenceType {
    if (type !is KaFunctionType) return ObjCIdType

    val objCBlockPointerType = ObjCBlockPointerType(
        returnType = if (typeBridge.returnsVoid) ObjCVoidType else translateToObjCReferenceType(type.returnType),
        parameterTypes = listOfNotNull(type.receiverType).plus(type.parameterTypes).map { parameterType ->
            translateToObjCReferenceType(parameterType)
        }
    )
    return analysisSession.withNullabilityOf(objCBlockPointerType, type)
}
