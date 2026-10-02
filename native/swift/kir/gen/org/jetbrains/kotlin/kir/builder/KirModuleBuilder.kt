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
import org.jetbrains.kotlin.kir.KirModule
import org.jetbrains.kotlin.kir.impl.KirModuleImpl

@KirBuilderDsl
class KirModuleBuilder {
    val functions: MutableList<KirFunction> = []

    fun build(): KirModule {
        return KirModuleImpl(
            functions,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildModule(init: KirModuleBuilder.() -> Unit = {}): KirModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KirModuleBuilder().apply(init).build()
}
