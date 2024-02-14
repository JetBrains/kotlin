package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.backend.konan.objcexport.*

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtType.translateToObjCFunctionType(typeBridge: BlockPointerBridge): ObjCReferenceType {
    if (this !is KtFunctionalType) return ObjCIdType

    return ObjCBlockPointerType(
        returnType = if (typeBridge.returnsVoid) ObjCVoidType else returnType.translateToObjCReferenceType(),
        parameterTypes = listOfNotNull(this.receiverType).plus(this.parameterTypes).map { parameterType ->
            parameterType.translateToObjCReferenceType()
        }
    ).withNullabilityOf(this)
}
