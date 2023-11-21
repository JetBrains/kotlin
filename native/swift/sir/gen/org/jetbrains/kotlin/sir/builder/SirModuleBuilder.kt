/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.sir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.sir.SirBuilderDsl
import org.jetbrains.kotlin.sir.SirDeclaration
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.impl.SirModuleImpl

@SirBuilderDsl
class SirModuleBuilder {
    val declarations: MutableList<SirDeclaration> = mutableListOf()

    fun build(): SirModule {
        return SirModuleImpl(
            declarations,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildModule(init: SirModuleBuilder.() -> Unit = {}): SirModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirModuleBuilder().apply(init).build()
}
