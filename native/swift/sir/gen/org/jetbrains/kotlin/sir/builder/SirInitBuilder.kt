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
    lateinit var kind: SirCallableKind
    var body: SirFunctionBody? = null
    var isFailable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val parameters: MutableList<SirParameter> = mutableListOf()
    lateinit var initKind: SirInitializerKind
    var isOverride: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    fun build(): SirInit {
        return SirInitImpl(
            origin,
            visibility,
            documentation,
            kind,
            body,
            isFailable,
            parameters,
            initKind,
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

@OptIn(ExperimentalContracts::class)
inline fun buildInitCopy(original: SirInit, init: SirInitBuilder.() -> Unit): SirInit {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirInitBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.documentation = original.documentation
    copyBuilder.kind = original.kind
    copyBuilder.body = original.body
    copyBuilder.isFailable = original.isFailable
    copyBuilder.parameters.addAll(original.parameters)
    copyBuilder.initKind = original.initKind
    copyBuilder.isOverride = original.isOverride
    return copyBuilder.apply(init).build()
}
