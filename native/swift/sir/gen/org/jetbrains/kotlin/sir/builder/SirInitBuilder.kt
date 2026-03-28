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
import org.jetbrains.kotlin.sir.impl.SirInitImpl

@SirBuilderDsl
class SirInitBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    var documentation: String? = null
    val attributes: MutableList<SirAttribute> = mutableListOf()
    val bridges: MutableList<SirBridge> = mutableListOf()
    var body: SirFunctionBody? = null
    var errorType: SirType = SirType.never
    var isAsync: Boolean = false
    var isFailable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val parameters: MutableList<SirParameter> = mutableListOf()
    var isConvenience: Boolean = false
    var isRequired: Boolean = false
    var isOverride: Boolean = false

    fun build(): SirInit {
        return SirInitImpl(
            origin,
            visibility,
            documentation,
            attributes,
            bridges,
            body,
            errorType,
            isAsync,
            isFailable,
            parameters,
            isConvenience,
            isRequired,
            isOverride,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildInit(init: SirInitBuilder.() -> Unit): SirInit {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirInitBuilder().apply(init).build()
}

@OptIn(SirImplementationDetail::class)
fun buildInit(
    origin: SirOrigin = SirOrigin.Unknown,
    visibility: SirVisibility = SirVisibility.PUBLIC,
    documentation: String? = null,
    attributes: MutableList<SirAttribute> = mutableListOf(),
    bridges: MutableList<SirBridge> = mutableListOf(),
    body: SirFunctionBody? = null,
    errorType: SirType = SirType.never,
    isAsync: Boolean = false,
    isFailable: Boolean,
    parameters: MutableList<SirParameter> = mutableListOf(),
    isConvenience: Boolean = false,
    isRequired: Boolean = false,
    isOverride: Boolean = false,
): SirInit {
    return SirInitImpl(
        origin,
        visibility,
        documentation,
        attributes,
        bridges,
        body,
        errorType,
        isAsync,
        isFailable,
        parameters,
        isConvenience,
        isRequired,
        isOverride,
    )
}

@OptIn(SirImplementationDetail::class)
fun buildInitCopy(
    original: SirInit,
    origin: SirOrigin = original.origin,
    visibility: SirVisibility = original.visibility,
    documentation: String? = original.documentation,
    attributes: MutableList<SirAttribute> = original.attributes.toMutableList(),
    bridges: MutableList<SirBridge> = original.bridges.toMutableList(),
    body: SirFunctionBody? = original.body,
    errorType: SirType = original.errorType,
    isAsync: Boolean = original.isAsync,
    isFailable: Boolean = original.isFailable,
    parameters: MutableList<SirParameter> = original.parameters.toMutableList(),
    isConvenience: Boolean = original.isConvenience,
    isRequired: Boolean = original.isRequired,
    isOverride: Boolean = original.isOverride,
): SirInit {
    return SirInitImpl(
        origin,
        visibility,
        documentation,
        attributes,
        bridges,
        body,
        errorType,
        isAsync,
        isFailable,
        parameters,
        isConvenience,
        isRequired,
        isOverride,
    )
}
