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
import org.jetbrains.kotlin.sir.impl.SirStructImpl

@SirBuilderDsl
class SirStructBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    lateinit var name: String
    val declarations: MutableList<SirDeclaration> = mutableListOf()

    fun build(): SirStruct {
        return SirStructImpl(
            origin,
            visibility,
            name,
            declarations,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildStruct(init: SirStructBuilder.() -> Unit): SirStruct {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirStructBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildStructCopy(original: SirStruct, init: SirStructBuilder.() -> Unit): SirStruct {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirStructBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.name = original.name
    copyBuilder.declarations.addAll(original.declarations)
    return copyBuilder.apply(init).build()
}
