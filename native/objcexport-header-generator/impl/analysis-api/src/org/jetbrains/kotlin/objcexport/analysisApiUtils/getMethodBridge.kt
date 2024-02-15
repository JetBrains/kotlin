package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.backend.konan.KonanPrimitiveType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.*

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.bridgeMethodImpl]
 */
context(KtAnalysisSession, KtObjCExportSession)
internal fun KtFunctionLikeSymbol.getFunctionMethodBridge(): MethodBridge {

    val valueParameters = mutableListOf<MethodBridgeValueParameter>()

    this.valueParameters.forEach {
        valueParameters += bridgeParameter(it.returnType)
    }

    if (this is KtFunctionSymbol && isSuspend) {
        valueParameters += MethodBridgeValueParameter.SuspendCompletion(false)
    }

    return MethodBridge(
        bridgeReturnType(),
        receiverType,
        valueParameters
    )
}

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtVariableLikeSymbol.getPropertyMethodBridge(): MethodBridge {
    return MethodBridge(
        bridgeReturnType(),
        receiverType,
        emptyList()
    )
}

context(KtAnalysisSession)
private val KtCallableSymbol.receiverType: MethodBridgeReceiver
    get() = if (isArrayConstructor) {
        MethodBridgeReceiver.Factory
    } else if (!isConstructor && isTopLevel) {
        MethodBridgeReceiver.Static
    } else {
        MethodBridgeReceiver.Instance
    }

/**
 * [ObjCExportMapper.bridgeParameter]
 */
context(KtAnalysisSession, KtObjCExportSession)
private fun bridgeParameter(type: KtType): MethodBridgeValueParameter {
    return MethodBridgeValueParameter.Mapped(bridgeType(type))
}

/**
 * [ObjCExportMapper.bridgeType]
 */
context(KtAnalysisSession)
private fun bridgeType(
    type: KtType,
): TypeBridge {
    val primitiveObjCValueType = when {
        type.isBoolean -> ObjCValueType.BOOL
        type.isChar -> ObjCValueType.UNICHAR
        type.isByte -> ObjCValueType.CHAR
        type.isShort -> ObjCValueType.SHORT
        type.isInt -> ObjCValueType.INT
        type.isLong -> ObjCValueType.LONG_LONG
        type.isFloat -> ObjCValueType.FLOAT
        type.isDouble -> ObjCValueType.DOUBLE
        type.isUByte -> ObjCValueType.UNSIGNED_CHAR
        type.isUShort -> ObjCValueType.UNSIGNED_SHORT
        type.isUInt -> ObjCValueType.UNSIGNED_INT
        type.isULong -> ObjCValueType.UNSIGNED_LONG_LONG
        type.isClassTypeWithClassId(KonanPrimitiveType.NON_NULL_NATIVE_PTR.classId) -> ObjCValueType.POINTER
        else -> null
    }

    if (primitiveObjCValueType != null) {
        return ValueTypeBridge(primitiveObjCValueType)
    }

    /* If type is inlined, then build the bridge for the inlined target type */
    type.getInlineTargetTypeOrNull()?.let { inlinedTargetType ->
        return bridgeType(inlinedTargetType)
    }

    if (type.isFunctionType) {
        return bridgeFunctionType(type)
    }

    return ReferenceBridge
}

/**
 * [ObjCExportMapper.bridgeFunctionType]
 */
context(KtAnalysisSession)
private fun bridgeFunctionType(type: KtType): TypeBridge {

    val numberOfParameters: Int
    val returnType: KtType

    if (type is KtFunctionalType) {
        numberOfParameters = type.parameterTypes.size
        returnType = type.returnType
    } else {
        numberOfParameters = 0
        returnType = type
    }

    val returnsVoid = returnType.isUnit || returnType.isNothing
    return BlockPointerBridge(numberOfParameters, returnsVoid)
}

/**
 * [ObjCExportMapper.bridgeReturnType]
 */
context(KtAnalysisSession, KtObjCExportSession)
private fun KtCallableSymbol.bridgeReturnType(): MethodBridge.ReturnValue {

    val convertExceptionsToErrors = false // TODO: Add exception handling and return MethodBridge.ReturnValue.WithError.ZeroForError

    if (isArrayConstructor) {
        return MethodBridge.ReturnValue.Instance.FactoryResult
    } else if (isConstructor) {
        val result = MethodBridge.ReturnValue.Instance.InitResult
        if (convertExceptionsToErrors) {
            MethodBridge.ReturnValue.WithError.ZeroForError(result, successMayBeZero = false)
        } else {
            return result
        }
    } else if (returnType.isSuspendFunctionType) {
        return MethodBridge.ReturnValue.Suspend
    }

    if (isHashCode) {
        return MethodBridge.ReturnValue.HashCode
    }

    //TODO: handle getter
//    descriptor is PropertyGetterDescriptor -> {
//        assert(!convertExceptionsToErrors)
//        MethodBridge.ReturnValue.Mapped(bridgePropertyType(descriptor.correspondingProperty))
//    }

    if (returnType.isUnit || returnType.isNothing) {
        return if (convertExceptionsToErrors) {
            MethodBridge.ReturnValue.WithError.Success
        } else {
            MethodBridge.ReturnValue.Void
        }
    }


    val returnTypeBridge = bridgeType(returnType)
    val successReturnValueBridge = MethodBridge.ReturnValue.Mapped(returnTypeBridge)

    return if (convertExceptionsToErrors) {
        val canReturnZero = !returnTypeBridge.isReferenceOrPointer() || returnType.canBeNull
        MethodBridge.ReturnValue.WithError.ZeroForError(
            successReturnValueBridge,
            successMayBeZero = canReturnZero
        )
    } else {
        successReturnValueBridge
    }
}

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.isReferenceOrPointer]
 */
private fun TypeBridge.isReferenceOrPointer(): Boolean = when (this) {
    ReferenceBridge, is BlockPointerBridge -> true
    is ValueTypeBridge -> this.objCValueType == ObjCValueType.POINTER
}
