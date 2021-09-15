/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.commonizer.utils.hashCode
import org.jetbrains.kotlin.commonizer.utils.isObjCInteropCallableAnnotation


typealias ObjCFunctionApproximation = Int

data class PropertyApproximationKey(
    val name: CirName,
    val extensionReceiverParameterType: CirTypeSignature?
) {
    companion object {
        internal fun create(
            property: CirProperty,
            signatureBuildingContext: SignatureBuildingContext
        ): PropertyApproximationKey {
            return PropertyApproximationKey(
                name = property.name,
                extensionReceiverParameterType = property.extensionReceiver?.let {
                    buildApproximationSignature(signatureBuildingContext, it.type)
                }
            )
        }
    }
}

data class FunctionApproximationKey(
    val name: CirName,
    val valueParametersTypes: Array<CirTypeSignature>,
    val extensionReceiverParameterType: CirTypeSignature?,
    val objCFunctionApproximation: ObjCFunctionApproximation
) {

    companion object {
        internal fun create(
            function: CirFunction,
            signatureBuildingContext: SignatureBuildingContext
        ): FunctionApproximationKey {
            return FunctionApproximationKey(
                name = function.name,
                valueParametersTypes = valueParameterTypes(function, signatureBuildingContext),
                extensionReceiverParameterType = function.extensionReceiver?.let {
                    buildApproximationSignature(signatureBuildingContext, it.type)
                },
                objCFunctionApproximation = objCFunctionApproximation(function)
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FunctionApproximationKey)
            return false

        return name == other.name
                && objCFunctionApproximation == other.objCFunctionApproximation
                && valueParametersTypes.contentEquals(other.valueParametersTypes)
                && extensionReceiverParameterType == other.extensionReceiverParameterType
    }

    override fun hashCode() = hashCode(name)
        .appendHashCode(valueParametersTypes)
        .appendHashCode(extensionReceiverParameterType)
        .appendHashCode(objCFunctionApproximation)
}


data class ConstructorApproximationKey(
    val valueParametersTypes: Array<CirTypeSignature>,
    private val objCFunctionApproximation: ObjCFunctionApproximation
) {

    companion object {
        internal fun create(
            constructor: CirClassConstructor, signatureBuildingContext: SignatureBuildingContext
        ): ConstructorApproximationKey {
            return ConstructorApproximationKey(
                valueParametersTypes = valueParameterTypes(constructor, signatureBuildingContext),
                objCFunctionApproximation = objCFunctionApproximation(constructor)
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ConstructorApproximationKey)
            return false

        return objCFunctionApproximation == other.objCFunctionApproximation
                && valueParametersTypes.contentEquals(other.valueParametersTypes)
    }

    override fun hashCode() = hashCode(valueParametersTypes)
        .appendHashCode(objCFunctionApproximation)
}

private fun <T> objCFunctionApproximation(value: T): ObjCFunctionApproximation
        where T : CirHasAnnotations, T : CirCallableMemberWithParameters {
    return if (value.annotations.any { it.type.classifierId.isObjCInteropCallableAnnotation }) {
        value.valueParameters.fold(0) { acc, next -> acc.appendHashCode(next.name) }
    } else 0
}

private fun <T> valueParameterTypes(
    callable: T,
    signatureBuildingContext: SignatureBuildingContext,
): Array<CirTypeSignature>
        where T : CirHasTypeParameters, T : CirCallableMemberWithParameters, T : CirMaybeCallableMemberOfClass {
    if (callable.valueParameters.isEmpty()) return emptyArray()
    return Array(callable.valueParameters.size) { index ->
        val parameter = callable.valueParameters[index]
        buildApproximationSignature(signatureBuildingContext, parameter.returnType)
    }
}
