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
import org.jetbrains.kotlin.sir.impl.SirEnumImpl

@SirBuilderDsl
class SirEnumBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    lateinit var name: String
    val declarations: MutableList<SirDeclaration> = mutableListOf()
    val cases: MutableList<SirEnumCase> = mutableListOf()

    fun build(): SirEnum {
        return SirEnumImpl(
            origin,
            visibility,
            name,
            declarations,
            cases,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildEnum(init: SirEnumBuilder.() -> Unit): SirEnum {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirEnumBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumCopy(original: SirEnum, init: SirEnumBuilder.() -> Unit): SirEnum {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirEnumBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.name = original.name
    copyBuilder.declarations.addAll(original.declarations)
    copyBuilder.cases.addAll(original.cases)
    return copyBuilder.apply(init).build()
}
