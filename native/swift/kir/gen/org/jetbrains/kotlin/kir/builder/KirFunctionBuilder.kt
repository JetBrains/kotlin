/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/kir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.kir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.kir.KirBuilderDsl
import org.jetbrains.kotlin.kir.KirFunction
import org.jetbrains.kotlin.kir.KirParameter
import org.jetbrains.kotlin.kir.impl.KirFunctionImpl

@KirBuilderDsl
class KirFunctionBuilder {
    lateinit var name: String
    val parameters: MutableList<KirParameter> = []

    fun build(): KirFunction {
        return KirFunctionImpl(
            name,
            parameters,
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
    copyBuilder.name = original.name
    copyBuilder.parameters.addAll(original.parameters)
    return copyBuilder.apply(init).build()
}
