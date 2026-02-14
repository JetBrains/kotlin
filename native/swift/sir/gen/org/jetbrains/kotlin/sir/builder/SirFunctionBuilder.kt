/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.sir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.impl.SirFunctionImpl

@SirBuilderDsl
class SirFunctionBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    var documentation: String? = null
    val attributes: MutableList<SirAttribute> = mutableListOf()
    val bridges: MutableList<SirBridge> = mutableListOf()
    var body: SirFunctionBody? = null
    var errorType: SirType = SirType.never
    var isAsync: Boolean = false
    var isOverride: Boolean = false
    var isInstance: Boolean = true
    var modality: SirModality = SirModality.UNSPECIFIED
    lateinit var name: String
    val contextParameters: MutableList<SirParameter> = mutableListOf()
    var extensionReceiverParameter: SirParameter? = null
    val parameters: MutableList<SirParameter> = mutableListOf()
    lateinit var returnType: SirType
    var fixity: SirFixity? = null

    fun build(): SirFunction {
        return SirFunctionImpl(
            origin,
            visibility,
            documentation,
            attributes,
            bridges,
            body,
            errorType,
            isAsync,
            isOverride,
            isInstance,
            modality,
            name,
            contextParameters,
            extensionReceiverParameter,
            parameters,
            returnType,
            fixity,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunction(init: SirFunctionBuilder.() -> Unit): SirFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirFunctionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionCopy(original: SirFunction, init: SirFunctionBuilder.() -> Unit): SirFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirFunctionBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.documentation = original.documentation
    copyBuilder.attributes.addAll(original.attributes)
    copyBuilder.bridges.addAll(original.bridges)
    copyBuilder.body = original.body
    copyBuilder.errorType = original.errorType
    copyBuilder.isAsync = original.isAsync
    copyBuilder.isOverride = original.isOverride
    copyBuilder.isInstance = original.isInstance
    copyBuilder.modality = original.modality
    copyBuilder.name = original.name
    copyBuilder.contextParameters.addAll(original.contextParameters)
    copyBuilder.extensionReceiverParameter = original.extensionReceiverParameter
    copyBuilder.parameters.addAll(original.parameters)
    copyBuilder.returnType = original.returnType
    copyBuilder.fixity = original.fixity
    return copyBuilder.apply(init).build()
}
