/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    lateinit var origin: SirOrigin
    lateinit var visibility: SirVisibility
    lateinit var name: String
    val parameters: MutableList<SirParameter> = mutableListOf()
    lateinit var returnType: SirType

    fun build(): SirFunction {
        return SirFunctionImpl(
            origin,
            visibility,
            name,
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
