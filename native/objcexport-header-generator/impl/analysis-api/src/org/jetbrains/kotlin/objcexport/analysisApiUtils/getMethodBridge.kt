/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.export.utilities.isHashCode
import org.jetbrains.kotlin.analysis.api.export.utilities.isSuspend
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.KonanPrimitiveType
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.*

/**
 * This method is tightly bound with [valueParametersAssociated] and order in [MethodBridge.valueParameters] matters.
 * K1 function descriptor has property [allParameters], but analysis API doesn't so we need to combine manually in exact order:
 * [KaFunctionSymbol.receiverParameter], [KaFunctionSymbol.valueParameters] and inner class edge case.
 * Then [valueParametersAssociated] associates parameters according the order.
 *
 * See K1 implementation [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapperKt.bridgeMethodImpl]
 */
internal fun ObjCExportContext.getFunctionMethodBridge(symbol: KaFunctionSymbol): MethodBridge {

    val valueParameters = mutableListOf<MethodBridgeValueParameter>()
    val isInner = (with(analysisSession) { symbol.containingDeclaration } as? KaNamedClassSymbol)?.isInner ?: false

    symbol.receiverParameter?.apply {
        valueParameters += bridgeParameter(this.returnType)
    }

    symbol.valueParameters.forEach {
        valueParameters += bridgeParameter(it.returnType)
    }

    if (isInner) {
        valueParameters += bridgeParameter(symbol.returnType)
    }

    if (symbol.isSuspend) {
        valueParameters += MethodBridgeValueParameter.SuspendCompletion(with(analysisSession) { symbol.returnType.isUnitType })
    } else if (symbol.hasThrowsAnnotation) {
        // Add error out parameter before tail block parameters. The convention allows this.
        // Placing it after would trigger https://bugs.swift.org/browse/SR-12201
        // (see also https://github.com/JetBrains/kotlin-native/issues/3825).
        val tailBlocksCount = valueParameters.reversed().takeWhile { it.isBlockPointer() }.count()
        valueParameters.add(valueParameters.size - tailBlocksCount, MethodBridgeValueParameter.ErrorOutParameter)
    }

    return MethodBridge(
        returnBridge = bridgeReturnType(symbol),
        receiver = analysisSession.getBridgeReceiverType(symbol),
        valueParameters = valueParameters
    )
}

internal fun KaSession.getBridgeReceiverType(symbol: KaCallableSymbol): MethodBridgeReceiver {
    return if (isArrayConstructor(symbol)) {
        MethodBridgeReceiver.Factory
    } else if (!symbol.isConstructor && isTopLevelCallable(symbol)) {
        MethodBridgeReceiver.Static
    } else {
        MethodBridgeReceiver.Instance
    }
}

/**
 * We can't use [KaSymbol.isTopLevel] directly since top level callables in Objective-C handled differently
 * So we use [isTopLevel] for most cases.
 * But there is one edge case is when extension receiver is containing class itself, hence property isn't top level:
 * ```kotlin
 * class Foo {
 *   val Foo.bar: Int get() = 42
 * }
 * ```
 */
private fun KaSession.isTopLevelCallable(symbol: KaCallableSymbol): Boolean {
    return if (symbol.containingSymbol is KaPropertySymbol) {
        return (symbol.containingSymbol as KaPropertySymbol).isTopLevel
    } else isTopLevel(symbol)
}

/**
 * [ObjCExportMapper.bridgeParameter]
 */
fun ObjCExportContext.bridgeParameter(type: KaType): MethodBridgeValueParameter {
    return MethodBridgeValueParameter.Mapped(analysisSession.bridgeType(type))
}

/**
 * [ObjCExportMapper.bridgeType]
 */
private fun KaSession.bridgeType(
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
        /**
         * Nullable primitives needs to be passed through priority mapping at [mapToReferenceTypeIgnoringNullability]
         * And either be translated as `id _Nullable` or nullable mapped type
         */
        return if (primitiveObjCValueType == ObjCValueType.POINTER) {
            ValueTypeBridge(primitiveObjCValueType)
        } else if (type.isMarkedNullable) ReferenceBridge else ValueTypeBridge(primitiveObjCValueType)
    }

    /* If type is inlined, then build the bridge for the inlined target type */
    getInlineTargetTypeOrNull(type)?.let { inlinedTargetType ->
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
private fun KaSession.bridgeFunctionType(type: KaType): TypeBridge {

    val numberOfParameters: Int
    val returnType: KaType

    if (type is KaFunctionType) {
        numberOfParameters = type.parameterTypes.size
        returnType = type.returnType
    } else {
        numberOfParameters = 0
        returnType = type
    }

    return BlockPointerBridge(numberOfParameters, isObjCVoid(returnType))
}

internal fun KaSession.isObjCVoid(type: KaType): Boolean {
    return type.isUnitType || type.isNothingType
}

/**
 * [ObjCExportMapper.bridgeReturnType]
 */
private fun ObjCExportContext.bridgeReturnType(symbol: KaCallableSymbol): MethodBridge.ReturnValue {
    val sessionReturnType = exportSession.exportSessionReturnType(symbol)

    if (analysisSession.isArrayConstructor(symbol)) {
        return MethodBridge.ReturnValue.Instance.FactoryResult
    } else if (symbol.isConstructor) {
        val result = MethodBridge.ReturnValue.Instance.InitResult
        if (symbol.hasThrowsAnnotation) {
            MethodBridge.ReturnValue.WithError.ZeroForError(result, successMayBeZero = false)
        } else {
            return result
        }
    } else if (symbol is KaFunctionSymbol && symbol.isSuspend) {
        return MethodBridge.ReturnValue.Suspend
    }

    if (analysisSession.isHashCode(symbol)) {
        return MethodBridge.ReturnValue.HashCode
    }

    if (with(analysisSession) { sessionReturnType.isUnitType }) {
        return symbol.successOrVoidReturnValue
    }

    if (analysisSession.isObjCNothing(sessionReturnType) && symbol !is KaPropertyAccessorSymbol) {
        return symbol.successOrVoidReturnValue
    }

    val returnTypeBridge = analysisSession.bridgeType(sessionReturnType)
    val successReturnValueBridge = MethodBridge.ReturnValue.Mapped(returnTypeBridge)

    return if (symbol.hasThrowsAnnotation) {
        val canReturnZero = !returnTypeBridge.isReferenceOrPointer() || with(analysisSession) { sessionReturnType.isNullable }
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
