package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCVoid
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal fun ObjCExportContext.translateToObjCFunctionType(type: KaType, returnsVoid: Boolean): ObjCReferenceType {
    if (type !is KaFunctionType) return ObjCIdType

    val allValueParameterTypes = listOfNotNull(type.receiverType).plus(type.parameterTypes)
    val objCBlockPointerType = ObjCBlockPointerType(
        returnType = if (returnsVoid) {
            ObjCVoidType
        } else {
            val returnType = type.getReturnTypeFromFunctionType()
            if (returnType is KaFunctionType) translateToObjCFunctionType(returnType, analysisSession.isObjCVoid(returnType))
            else translateToObjCReferenceType(returnType)
        },
        parameterTypes = allValueParameterTypes.map { parameterType ->
            translateToObjCReferenceType(parameterType)
        },
        extras = mutableExtrasOf().apply {
            originParameterNames = allValueParameterTypes.map { parameterType ->
                val parameterTypeArgument =
                    parameterType.annotations.firstOrNull()?.arguments?.firstOrNull()?.expression as? KaAnnotationValue.ConstantValue
                (parameterTypeArgument?.value?.value as? String) ?: ""
            }
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