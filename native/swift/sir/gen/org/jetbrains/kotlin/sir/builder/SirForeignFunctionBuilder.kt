/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.sir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.sir.SirBuilderDsl
import org.jetbrains.kotlin.sir.SirForeignFunction
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.impl.SirForeignFunctionImpl

@SirBuilderDsl
class SirForeignFunctionBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC

    fun build(): SirForeignFunction {
        return SirForeignFunctionImpl(
            origin,
            visibility,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildForeignFunction(init: SirForeignFunctionBuilder.() -> Unit = {}): SirForeignFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirForeignFunctionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildForeignFunctionCopy(original: SirForeignFunction, init: SirForeignFunctionBuilder.() -> Unit = {}): SirForeignFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirForeignFunctionBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    return copyBuilder.apply(init).build()
}
