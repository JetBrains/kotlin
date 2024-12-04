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
    var body: SirFunctionBody? = null
    var errorType: SirType = SirType.never
    var isOverride: Boolean = false
    var isInstance: Boolean = true
    var modality: SirModality = SirModality.UNSPECIFIED
    lateinit var name: String
    var extensionReceiverParameter: SirParameter? = null
    val parameters: MutableList<SirParameter> = mutableListOf()
    lateinit var returnType: SirType

    fun build(): SirFunction {
        return SirFunctionImpl(
            origin,
            visibility,
            documentation,
            attributes,
            body,
            errorType,
            isOverride,
            isInstance,
            modality,
            name,
            extensionReceiverParameter,
            parameters,
            returnType,
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
    copyBuilder.body = original.body
    copyBuilder.errorType = original.errorType
    copyBuilder.isOverride = original.isOverride
    copyBuilder.isInstance = original.isInstance
    copyBuilder.modality = original.modality
    copyBuilder.name = original.name
    copyBuilder.extensionReceiverParameter = original.extensionReceiverParameter
    copyBuilder.parameters.addAll(original.parameters)
    copyBuilder.returnType = original.returnType
    return copyBuilder.apply(init).build()
}
