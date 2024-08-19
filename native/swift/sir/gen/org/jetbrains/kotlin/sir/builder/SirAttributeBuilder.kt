/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.sir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.sir.SirAttribute
import org.jetbrains.kotlin.sir.SirBuilderDsl
import org.jetbrains.kotlin.sir.impl.SirAttributeImpl

@SirBuilderDsl
class SirAttributeBuilder {
    lateinit var name: String
    val arguments: MutableList<String> = mutableListOf()

    fun build(): SirAttribute {
        return SirAttributeImpl(
            name,
            arguments,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildAttribute(init: SirAttributeBuilder.() -> Unit): SirAttribute {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirAttributeBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildAttributeCopy(original: SirAttribute, init: SirAttributeBuilder.() -> Unit): SirAttribute {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirAttributeBuilder()
    copyBuilder.name = original.name
    copyBuilder.arguments.addAll(original.arguments)
    return copyBuilder.apply(init).build()
}
