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
import org.jetbrains.kotlin.sir.impl.SirEnumCaseImpl

@SirBuilderDsl
class SirEnumCaseBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    var documentation: String? = null
    val attributes: MutableList<SirAttribute> = mutableListOf()
    val bridges: MutableList<SirBridge> = mutableListOf()
    lateinit var name: String

    fun build(): SirEnumCase {
        return SirEnumCaseImpl(
            origin,
            visibility,
            documentation,
            attributes,
            bridges,
            name,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumCase(init: SirEnumCaseBuilder.() -> Unit): SirEnumCase {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirEnumCaseBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildEnumCaseCopy(original: SirEnumCase, init: SirEnumCaseBuilder.() -> Unit): SirEnumCase {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirEnumCaseBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.documentation = original.documentation
    copyBuilder.attributes.addAll(original.attributes)
    copyBuilder.bridges.addAll(original.bridges)
    copyBuilder.name = original.name
    return copyBuilder.apply(init).build()
}
