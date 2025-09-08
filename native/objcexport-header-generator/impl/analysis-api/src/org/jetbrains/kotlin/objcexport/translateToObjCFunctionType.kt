package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.export.utilities.getValueFromParameterNameAnnotation
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCVoid
import org.jetbrains.kotlin.objcexport.mangling.unifyName

internal fun ObjCExportContext.translateToObjCFunctionType(type: KaType, returnsVoid: Boolean): ObjCReferenceType {
    if (type !is KaFunctionType) return ObjCIdType
    val usedNames = mutableSetOf<String>()
    val objCBlockPointerType = ObjCBlockPointerType(
        returnType = if (returnsVoid) {
            ObjCVoidType
        } else {
            val returnType = type.getReturnTypeFromFunctionType()
            if (returnType is KaFunctionType) translateToObjCFunctionType(returnType, analysisSession.isObjCVoid(returnType))
            else translateToObjCReferenceType(returnType)
        },
        parameters = listOfNotNull(type.receiverType).plus(type.parameterTypes).map { parameterType ->
            val name = blockParameterName(parameterType, usedNames)
            ObjCParameter(
                name,
                null,
                type = translateToObjCReferenceType(parameterType),
                todo = null,
            )
        }
    )
    return analysisSession.withNullabilityOf(objCBlockPointerType, type)
}

private fun ObjCExportContext.blockParameterName(parameterType: KaType, usedNames: MutableSet<String>): String {
    return if (this.exportSession.configuration.objcExportBlockExplicitParameterNames) {
        val parameterName = parameterType.getValueFromParameterNameAnnotation()?.asString()
            ?: return ""
        val mangledName = unifyName(parameterName, usedNames)
        usedNames += mangledName
        mangledName
    } else ""
}

/**
 * See K1 [org.jetbrains.kotlin.builtins.FunctionTypesKt.getReturnTypeFromFunctionType]
 */
private fun KaFunctionType.getReturnTypeFromFunctionType(): KaType {
    return this.typeArguments.last().type ?: error("Must never happen since typeArguments of KaFunctionType always contain at least Unit")
}