/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter

internal sealed class TypeBridge
internal object ReferenceBridge : TypeBridge()

internal data class BlockPointerBridge(
        val numberOfParameters: Int,
        val returnsVoid: Boolean
) : TypeBridge()

internal data class ValueTypeBridge(val objCValueType: ObjCValueType) : TypeBridge()

internal sealed class MethodBridgeParameter

internal sealed class MethodBridgeReceiver : MethodBridgeParameter() {
    object Static : MethodBridgeReceiver()
    object Factory : MethodBridgeReceiver()
    object Instance : MethodBridgeReceiver()
}

internal object MethodBridgeSelector : MethodBridgeParameter()

internal sealed class MethodBridgeValueParameter : MethodBridgeParameter() {
    data class Mapped(val bridge: TypeBridge) : MethodBridgeValueParameter()
    object ErrorOutParameter : MethodBridgeValueParameter()
    object SuspendCompletion : MethodBridgeValueParameter()
}

internal data class MethodBridge(
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
    val isInstance: Boolean get() = when (receiver) {
        MethodBridgeReceiver.Static,
        MethodBridgeReceiver.Factory -> false

        MethodBridgeReceiver.Instance -> true
    }

    val returnsError: Boolean
        get() = returnBridge is ReturnValue.WithError
}

internal fun MethodBridge.valueParametersAssociated(
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

            MethodBridgeValueParameter.SuspendCompletion,
            is MethodBridgeValueParameter.ErrorOutParameter -> it to null
        }
    }.also { assert(!kotlinParameters.hasNext()) }
}

internal fun MethodBridge.parametersAssociated(
        irFunction: IrFunction
): List<Pair<MethodBridgeParameter, IrValueParameter?>> {
    val kotlinParameters = irFunction.allParameters.iterator()

    return this.paramBridges.map {
        when (it) {
            is MethodBridgeValueParameter.Mapped, MethodBridgeReceiver.Instance ->
                it to kotlinParameters.next()

            MethodBridgeValueParameter.SuspendCompletion,
            MethodBridgeReceiver.Static, MethodBridgeSelector, MethodBridgeValueParameter.ErrorOutParameter ->
                it to null

            MethodBridgeReceiver.Factory -> {
                kotlinParameters.next()
                it to null
            }
        }
    }.also { assert(!kotlinParameters.hasNext()) }
}
