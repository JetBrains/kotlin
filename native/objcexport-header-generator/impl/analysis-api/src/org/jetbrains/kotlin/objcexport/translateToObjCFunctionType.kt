package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCVoid

internal fun ObjCExportContext.translateToObjCFunctionType(type: KaType, returnsVoid: Boolean): ObjCReferenceType {
    if (type !is KaFunctionType) return ObjCIdType
    val objCBlockPointerType = ObjCBlockPointerType(
        returnType = if (returnsVoid) {
            ObjCVoidType
        } else {
            val returnType = type.getReturnTypeFromFunctionType()
            if (returnType is KaFunctionType) translateToObjCFunctionType(returnType, analysisSession.isObjCVoid(returnType))
            else translateToObjCReferenceType(returnType)
        },
        parameterTypes = listOfNotNull(type.receiverType).plus(type.parameterTypes).map { parameterType ->
            translateToObjCReferenceType(parameterType)
        }
    )
    return analysisSession.withNullabilityOf(objCBlockPointerType, type)
}

/**
 * See K1 [org.jetbrains.kotlin.builtins.FunctionTypesKt.getReturnTypeFromFunctionType]
 */
private fun KaFunctionType.getReturnTypeFromFunctionType(): KaType {
    return this.typeArguments.last().type ?: error("Must never happen since typeArguments of KaFunctionType always contain at least Unit")
}