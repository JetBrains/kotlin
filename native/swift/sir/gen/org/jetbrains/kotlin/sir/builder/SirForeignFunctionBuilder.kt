/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.sir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirBuilderDsl
import org.jetbrains.kotlin.sir.SirForeignFunction
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.impl.SirForeignFunctionImpl

@SirBuilderDsl
class SirForeignFunctionBuilder {
    lateinit var origin: SirOrigin
    lateinit var visibility: SirVisibility

    fun build(): SirForeignFunction {
        return SirForeignFunctionImpl(
            origin,
            visibility,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildForeignFunction(init: SirForeignFunctionBuilder.() -> Unit): SirForeignFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirForeignFunctionBuilder().apply(init).build()
}
