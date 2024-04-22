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
import org.jetbrains.kotlin.sir.impl.SirClassImpl

@SirBuilderDsl
class SirClassBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    var documentation: String? = null
    lateinit var name: String
    val declarations: MutableList<SirDeclaration> = mutableListOf()
    var superClass: SirType? = null

    fun build(): SirClass {
        return SirClassImpl(
            origin,
            visibility,
            documentation,
            name,
            declarations,
            superClass,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildClass(init: SirClassBuilder.() -> Unit): SirClass {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirClassBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildClassCopy(original: SirClass, init: SirClassBuilder.() -> Unit): SirClass {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirClassBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.documentation = original.documentation
    copyBuilder.name = original.name
    copyBuilder.declarations.addAll(original.declarations)
    copyBuilder.superClass = original.superClass
    return copyBuilder.apply(init).build()
}
