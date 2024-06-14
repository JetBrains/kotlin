package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.objcexport.*

context(KaSession, KtObjCExportSession)
internal fun KaType.translateToObjCFunctionType(typeBridge: BlockPointerBridge): ObjCReferenceType {
    if (this !is KaFunctionType) return ObjCIdType

    return ObjCBlockPointerType(
        returnType = if (typeBridge.returnsVoid) ObjCVoidType else returnType.translateToObjCReferenceType(),
        parameterTypes = listOfNotNull(this.receiverType).plus(this.parameterTypes).map { parameterType ->
            parameterType.translateToObjCReferenceType()
        }
    ).withNullabilityOf(this)
}
