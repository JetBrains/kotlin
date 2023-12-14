/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor


@InternalKotlinNativeApi
fun MethodBridge.valueParametersAssociated(
    descriptor: FunctionDescriptor,
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
            is MethodBridgeValueParameter.ErrorOutParameter,
            -> it to null
        }
    }.also { assert(!kotlinParameters.hasNext()) }
}
