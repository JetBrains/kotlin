/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi

@InternalKotlinNativeApi
sealed class TypeBridge

@InternalKotlinNativeApi
object ReferenceBridge : TypeBridge()

@InternalKotlinNativeApi
data class BlockPointerBridge(
    val numberOfParameters: Int,
    val returnsVoid: Boolean,
) : TypeBridge()

@InternalKotlinNativeApi
data class ValueTypeBridge(val objCValueType: ObjCValueType) : TypeBridge()

@InternalKotlinNativeApi
sealed class MethodBridgeParameter

@InternalKotlinNativeApi
sealed class MethodBridgeReceiver : MethodBridgeParameter() {
    object Static : MethodBridgeReceiver()
    object Factory : MethodBridgeReceiver()
    object Instance : MethodBridgeReceiver()
}

@InternalKotlinNativeApi
object MethodBridgeSelector : MethodBridgeParameter()

@InternalKotlinNativeApi
sealed class MethodBridgeValueParameter : MethodBridgeParameter() {
    data class Mapped(val bridge: TypeBridge) : MethodBridgeValueParameter()
    object ErrorOutParameter : MethodBridgeValueParameter()
    data class SuspendCompletion(val useUnitCompletion: Boolean) : MethodBridgeValueParameter()
}

@InternalKotlinNativeApi
data class MethodBridge(
    val returnBridge: ReturnValue,
    val receiver: MethodBridgeReceiver,
    val valueParameters: List<MethodBridgeValueParameter>,
) {

    sealed class ReturnValue {
        object Void : ReturnValue()
        object HashCode : ReturnValue()
        data class Mapped(val bridge: TypeBridge) : ReturnValue()
        sealed class Instance : ReturnValue() {
            object InitResult : Instance()
            object FactoryResult : Instance()
        }

        sealed class WithError : ReturnValue() {
            object Success : WithError()
            data class ZeroForError(val successBridge: ReturnValue, val successMayBeZero: Boolean) : WithError()
        }

        object Suspend : ReturnValue()
    }

    val paramBridges: List<MethodBridgeParameter> =
        listOf(receiver) + MethodBridgeSelector + valueParameters

    // TODO: it is not exactly true in potential future cases.
    val isInstance: Boolean
        get() = when (receiver) {
            MethodBridgeReceiver.Static,
            MethodBridgeReceiver.Factory,
            -> false

            MethodBridgeReceiver.Instance -> true
        }

    val returnsError: Boolean
        get() = returnBridge is ReturnValue.WithError
}
