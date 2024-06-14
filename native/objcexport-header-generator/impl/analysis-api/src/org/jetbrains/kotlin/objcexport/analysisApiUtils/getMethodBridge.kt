package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.KonanPrimitiveType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.*

/**
 * This method is tightly bound with [valueParametersAssociated] and order in [MethodBridge.valueParameters] matters.
 * K1 function descriptor has property [allParameters], but analysis API doesn't so we need to combine manually in exact order:
 * [KtFunctionLikeSymbol.receiverParameter], [KtFunctionLikeSymbol.valueParameters] and inner class edge case.
 * Then [valueParametersAssociated] associates parameters according the order.
 *
 * See K1 implementation [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.bridgeMethodImpl]
 */
context(KaSession, KtObjCExportSession)
internal fun KaFunctionLikeSymbol.getFunctionMethodBridge(): MethodBridge {

    val valueParameters = mutableListOf<MethodBridgeValueParameter>()
    val isInner = (containingSymbol as? KaNamedClassOrObjectSymbol)?.isInner ?: false

    this.receiverParameter?.apply {
        valueParameters += bridgeParameter(this.type)
    }

    this.valueParameters.forEach {
        valueParameters += bridgeParameter(it.returnType)
    }

    if (isInner) {
        valueParameters += bridgeParameter(this.returnType)
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
        bridgeReceiverType,
        valueParameters
    )
}

context(KaSession)
internal val KaCallableSymbol.bridgeReceiverType: MethodBridgeReceiver
    get() {
        return if (isArrayConstructor) {
            MethodBridgeReceiver.Factory
        } else if (!isConstructor && isTopLevel && !isExtension) {
            MethodBridgeReceiver.Static
        } else {
            MethodBridgeReceiver.Instance
        }
    }

/**
 * [ObjCExportMapper.bridgeParameter]
 */
context(KaSession, KtObjCExportSession)
private fun bridgeParameter(type: KaType): MethodBridgeValueParameter {
    return MethodBridgeValueParameter.Mapped(bridgeType(type))
}

/**
 * [ObjCExportMapper.bridgeType]
 */
context(KaSession)
private fun bridgeType(
    type: KaType,
): TypeBridge {
    val primitiveObjCValueType = when {
        type.isBooleanType -> ObjCValueType.BOOL
        type.isCharType -> ObjCValueType.UNICHAR
        type.isByteType -> ObjCValueType.CHAR
        type.isShortType -> ObjCValueType.SHORT
        type.isIntType -> ObjCValueType.INT
        type.isLongType -> ObjCValueType.LONG_LONG
        type.isFloatType -> ObjCValueType.FLOAT
        type.isDoubleType -> ObjCValueType.DOUBLE
        type.isUByteType -> ObjCValueType.UNSIGNED_CHAR
        type.isUShortType -> ObjCValueType.UNSIGNED_SHORT
        type.isUIntType -> ObjCValueType.UNSIGNED_INT
        type.isULongType -> ObjCValueType.UNSIGNED_LONG_LONG
        type.isClassType(KonanPrimitiveType.VECTOR128.classId) && !type.isMarkedNullable ->
            ObjCValueType.VECTOR_FLOAT_128
        type.isClassType(KonanPrimitiveType.NON_NULL_NATIVE_PTR.classId) -> ObjCValueType.POINTER
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
context(KaSession)
private fun bridgeFunctionType(type: KaType): TypeBridge {

    val numberOfParameters: Int
    val returnType: KaType

    if (type is KaFunctionType) {
        numberOfParameters = type.parameterTypes.size
        returnType = type.returnType
    } else {
        numberOfParameters = 0
        returnType = type
    }

    val returnsVoid = returnType.isUnitType || returnType.isNothingType
    return BlockPointerBridge(numberOfParameters, returnsVoid)
}

/**
 * [ObjCExportMapper.bridgeReturnType]
 */
context(KaSession, KtObjCExportSession)
private fun KaCallableSymbol.bridgeReturnType(): MethodBridge.ReturnValue {

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

    if (returnType.isUnitType) {
        return successOrVoidReturnValue
    }

    if (returnType.isObjCNothing && this !is KaPropertyAccessorSymbol) {
        return successOrVoidReturnValue
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

private val KaCallableSymbol.successOrVoidReturnValue: MethodBridge.ReturnValue
    get() {
        return if (hasThrowsAnnotation) MethodBridge.ReturnValue.WithError.Success
        else MethodBridge.ReturnValue.Void
    }