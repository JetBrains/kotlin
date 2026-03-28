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
import org.jetbrains.kotlin.sir.impl.SirGetterImpl

@SirBuilderDsl
class SirGetterBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    var documentation: String? = null
    val attributes: MutableList<SirAttribute> = mutableListOf()
    val bridges: MutableList<SirBridge> = mutableListOf()
    var body: SirFunctionBody? = null
    var errorType: SirType = SirType.never
    var isAsync: Boolean = false

    fun build(): SirGetter {
        return SirGetterImpl(
            origin,
            visibility,
            documentation,
            attributes,
            bridges,
            body,
            errorType,
            isAsync,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildGetter(init: SirGetterBuilder.() -> Unit): SirGetter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirGetterBuilder().apply(init).build()
}

@OptIn(SirImplementationDetail::class)
fun buildGetter(
    origin: SirOrigin = SirOrigin.Unknown,
    visibility: SirVisibility = SirVisibility.PUBLIC,
    documentation: String? = null,
    attributes: MutableList<SirAttribute> = mutableListOf(),
    bridges: MutableList<SirBridge> = mutableListOf(),
    body: SirFunctionBody? = null,
    errorType: SirType = SirType.never,
    isAsync: Boolean = false,
): SirGetter {
    return SirGetterImpl(
        origin,
        visibility,
        documentation,
        attributes,
        bridges,
        body,
        errorType,
        isAsync,
    )
}

@OptIn(SirImplementationDetail::class)
fun buildGetterCopy(
    original: SirGetter,
    origin: SirOrigin = original.origin,
    visibility: SirVisibility = original.visibility,
    documentation: String? = original.documentation,
    attributes: MutableList<SirAttribute> = original.attributes.toMutableList(),
    bridges: MutableList<SirBridge> = original.bridges.toMutableList(),
    body: SirFunctionBody? = original.body,
    errorType: SirType = original.errorType,
    isAsync: Boolean = original.isAsync,
): SirGetter {
    return SirGetterImpl(
        origin,
        visibility,
        documentation,
        attributes,
        bridges,
        body,
        errorType,
        isAsync,
    )
}
