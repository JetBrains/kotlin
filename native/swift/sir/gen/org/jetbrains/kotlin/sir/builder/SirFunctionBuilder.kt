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
    var contextParameter: SirParameter? = null
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
            contextParameter,
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

@OptIn(SirImplementationDetail::class)
fun buildFunctionCopy(
    original: SirFunction,
    origin: SirOrigin = original.origin,
    visibility: SirVisibility = original.visibility,
    documentation: String? = original.documentation,
    attributes: MutableList<SirAttribute> = original.attributes.toMutableList(),
    bridges: MutableList<SirBridge> = original.bridges.toMutableList(),
    body: SirFunctionBody? = original.body,
    errorType: SirType = original.errorType,
    isAsync: Boolean = original.isAsync,
    isOverride: Boolean = original.isOverride,
    isInstance: Boolean = original.isInstance,
    modality: SirModality = original.modality,
    name: String = original.name,
    contextParameter: SirParameter? = original.contextParameter,
    extensionReceiverParameter: SirParameter? = original.extensionReceiverParameter,
    parameters: MutableList<SirParameter> = original.parameters.toMutableList(),
    returnType: SirType = original.returnType,
    fixity: SirFixity? = original.fixity,
): SirFunction {
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
        contextParameter,
        extensionReceiverParameter,
        parameters,
        returnType,
        fixity,
    )
}
