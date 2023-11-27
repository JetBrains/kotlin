/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.konan.InternalKonanApi
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.util.allParameters

@InternalKonanApi
sealed class TypeBridge

@InternalKonanApi
object ReferenceBridge : TypeBridge()

@InternalKonanApi
data class BlockPointerBridge(
        val numberOfParameters: Int,
        val returnsVoid: Boolean
) : TypeBridge()

@InternalKonanApi
data class ValueTypeBridge(val objCValueType: ObjCValueType) : TypeBridge()

@InternalKonanApi
sealed class MethodBridgeParameter

@InternalKonanApi
sealed class MethodBridgeReceiver : MethodBridgeParameter() {
    object Static : MethodBridgeReceiver()
    object Factory : MethodBridgeReceiver()
    object Instance : MethodBridgeReceiver()
}

@InternalKonanApi
object MethodBridgeSelector : MethodBridgeParameter()

@InternalKonanApi
sealed class MethodBridgeValueParameter : MethodBridgeParameter() {
    data class Mapped(val bridge: TypeBridge) : MethodBridgeValueParameter()
    object ErrorOutParameter : MethodBridgeValueParameter()
    data class SuspendCompletion(val useUnitCompletion: Boolean) : MethodBridgeValueParameter()
}

@InternalKonanApi
data class MethodBridge(
        val returnBridge: ReturnValue,
        val receiver: MethodBridgeReceiver,
        val valueParameters: List<MethodBridgeValueParameter>
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
            MethodBridgeReceiver.Factory -> false

            MethodBridgeReceiver.Instance -> true
        }

    val returnsError: Boolean
        get() = returnBridge is ReturnValue.WithError
}

@InternalKonanApi
fun MethodBridge.valueParametersAssociated(
        descriptor: FunctionDescriptor
): List<Pair<MethodBridgeValueParameter, ParameterDescriptor?>> {
    val kotlinParameters = descriptor.allParameters.iterator()
    val skipFirstKotlinParameter = when (this.receiver) {
        MethodBridgeReceiver.Static -> false
        MethodBridgeReceiver.Factory, MethodBridgeReceiver.Instance -> true
    }
    if (skipFirstKotlinParameter) {
        kotlinParameters.next()
    }

    return this.valueParameters.map {
        when (it) {
            is MethodBridgeValueParameter.Mapped -> it to kotlinParameters.next()

            is MethodBridgeValueParameter.SuspendCompletion,
            is MethodBridgeValueParameter.ErrorOutParameter -> it to null
        }
    }.also { assert(!kotlinParameters.hasNext()) }
}

@InternalKonanApi
fun MethodBridge.parametersAssociated(
        irFunction: IrFunction
): List<Pair<MethodBridgeParameter, IrValueParameter?>> {
    val kotlinParameters = irFunction.allParameters.iterator()

    return this.paramBridges.map {
        when (it) {
            is MethodBridgeValueParameter.Mapped,
            MethodBridgeReceiver.Instance,
            is MethodBridgeValueParameter.SuspendCompletion ->
                it to kotlinParameters.next()

            MethodBridgeReceiver.Static, MethodBridgeSelector, MethodBridgeValueParameter.ErrorOutParameter ->
                it to null

            MethodBridgeReceiver.Factory -> {
                kotlinParameters.next()
                it to null
            }
        }
    }.also { assert(!kotlinParameters.hasNext()) }
}
