/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/kir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.kir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.kir.*
import org.jetbrains.kotlin.kir.impl.KirFunctionImpl

@KirBuilderDsl
class KirFunctionBuilder {
    var origin: KirOrigin = KirOrigin.Unknown
    var visibility: KirVisibility = KirVisibility.PUBLIC
    var documentation: String? = null
    val bridges: MutableList<KirBridge> = []
    var body: KirFunctionBody? = null
    lateinit var name: String

    fun build(): KirFunction {
        return KirFunctionImpl(
            origin,
            visibility,
            documentation,
            bridges,
            body,
            name,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunction(init: KirFunctionBuilder.() -> Unit): KirFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KirFunctionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionCopy(original: KirFunction, init: KirFunctionBuilder.() -> Unit): KirFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = KirFunctionBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.documentation = original.documentation
    copyBuilder.bridges.addAll(original.bridges)
    copyBuilder.body = original.body
    copyBuilder.name = original.name
    return copyBuilder.apply(init).build()
}
