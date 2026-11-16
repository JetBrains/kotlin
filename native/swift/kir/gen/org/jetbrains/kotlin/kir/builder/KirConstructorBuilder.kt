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
import org.jetbrains.kotlin.kir.impl.KirConstructorImpl

@KirBuilderDsl
class KirConstructorBuilder {
    var origin: KirOrigin = KirOrigin.Unknown
    var visibility: KirVisibility = KirVisibility.PUBLIC
    var documentation: String? = null
    val bridges: MutableList<KirBridge> = []
    var body: KirFunctionBody? = null
    lateinit var delegationCallArguments: KirFunctionBody

    fun build(): KirConstructor {
        return KirConstructorImpl(
            origin,
            visibility,
            documentation,
            bridges,
            body,
            delegationCallArguments,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildConstructor(init: KirConstructorBuilder.() -> Unit): KirConstructor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KirConstructorBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildConstructorCopy(original: KirConstructor, init: KirConstructorBuilder.() -> Unit): KirConstructor {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = KirConstructorBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.documentation = original.documentation
    copyBuilder.bridges.addAll(original.bridges)
    copyBuilder.body = original.body
    copyBuilder.delegationCallArguments = original.delegationCallArguments
    return copyBuilder.apply(init).build()
}
