/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.CirFunction
import org.jetbrains.kotlin.commonizer.cir.CirProperty
import org.jetbrains.kotlin.commonizer.cir.CirType
import org.jetbrains.kotlin.commonizer.cir.CirValueParameter.Companion.copyInterned

interface CirTypeSubstitutor {
    fun substitute(targetIndex: Int, type: CirType): CirType
}

fun CirTypeSubstitutor.substitute(index: Int, property: CirProperty): CirProperty {
    val newReturnType = substitute(index, property.returnType)
    return if (newReturnType != property.returnType) property.copy(returnType = newReturnType) else property
}

fun CirTypeSubstitutor.substitute(index: Int, function: CirFunction): CirFunction {
    val newExtensionReceiverType = function.extensionReceiver?.type?.let { substitute(index, it) }

    val newExtensionReceiver = if (newExtensionReceiverType != function.extensionReceiver?.type)
        function.extensionReceiver?.copy(type = checkNotNull(newExtensionReceiverType))
    else function.extensionReceiver

    val newValueParameters = function.valueParameters.map { valueParameter ->
        val newReturnType = substitute(index, valueParameter.returnType)
        if (newReturnType != valueParameter.returnType) valueParameter.copyInterned(returnType = newReturnType) else valueParameter
    }

    val newReturnType = substitute(index, function.returnType)

    return if (
        newExtensionReceiver != function.extensionReceiver ||
        newValueParameters != function.valueParameters ||
        newReturnType != function.returnType
    ) function.copy(
        extensionReceiver = newExtensionReceiver,
        valueParameters = newValueParameters,
        returnType = newReturnType
    ) else function
}