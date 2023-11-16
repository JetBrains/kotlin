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
import org.jetbrains.kotlin.sir.impl.SwiftIrClassImpl

@SwiftIrBuilderDsl
class SwiftIrClassBuilder {
    lateinit var origin: Origin
    val attributes: MutableList<Attribute> = mutableListOf()
    lateinit var visibility: SwiftVisibility
    lateinit var name: String
    val declarations: MutableList<SwiftIrDeclaration> = mutableListOf()

    fun build(): SwiftIrClass {
        return SwiftIrClassImpl(
            origin,
            attributes,
            visibility,
            name,
            declarations,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildClass(init: SwiftIrClassBuilder.() -> Unit): SwiftIrClass {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SwiftIrClassBuilder().apply(init).build()
}
