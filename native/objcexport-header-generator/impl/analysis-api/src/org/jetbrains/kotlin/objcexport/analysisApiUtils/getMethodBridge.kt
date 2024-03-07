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

    if (isSuspend) {
        valueParameters += MethodBridgeValueParameter.SuspendCompletion(true)
    } else if (hasThrowsAnnotation) {
        // Add error out parameter before tail block parameters. The convention allows this.
        // Placing it after would trigger https://bugs.swift.org/browse/SR-12201
        // (see also https://github.com/JetBrains/kotlin-native/issues/3825).
        val tailBlocksCount = valueParameters.reversed().takeWhile { it.isBlockPointer() }.count()
        valueParameters.add(valueParameters.size - tailBlocksCount, MethodBridgeValueParameter.ErrorOutParameter)
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
    get() {
        val isProperty = getContainingSymbol() as? KtPropertySymbol != null
        return if (isArrayConstructor) {
            MethodBridgeReceiver.Factory
        } else if (!isConstructor && isTopLevel && !isExtension && !isProperty) {
            MethodBridgeReceiver.Static
        } else {
            MethodBridgeReceiver.Instance
        }
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

    if (isArrayConstructor) {
        return MethodBridge.ReturnValue.Instance.FactoryResult
    } else if (isConstructor) {
        val result = MethodBridge.ReturnValue.Instance.InitResult
        if (hasThrowsAnnotation) {
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

    if (returnType.isUnit || returnType.isNothing) {
        return if (hasThrowsAnnotation) {
            MethodBridge.ReturnValue.WithError.Success
        } else {
            MethodBridge.ReturnValue.Void
        }
    }


    val returnTypeBridge = bridgeType(returnType)
    val successReturnValueBridge = MethodBridge.ReturnValue.Mapped(returnTypeBridge)

    return if (hasThrowsAnnotation) {
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

private fun MethodBridgeValueParameter.isBlockPointer(): Boolean = when (this) {
    is MethodBridgeValueParameter.Mapped -> when (this.bridge) {
        ReferenceBridge, is ValueTypeBridge -> false
        is BlockPointerBridge -> true
    }
    MethodBridgeValueParameter.ErrorOutParameter -> false
    is MethodBridgeValueParameter.SuspendCompletion -> true
}