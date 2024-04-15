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
import org.jetbrains.kotlin.sir.impl.SirTypealiasImpl

@SirBuilderDsl
class SirTypealiasBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    var documentation: String? = null
    lateinit var name: String
    lateinit var type: SirType

    fun build(): SirTypealias {
        return SirTypealiasImpl(
            origin,
            visibility,
            documentation,
            name,
            type,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildTypealias(init: SirTypealiasBuilder.() -> Unit): SirTypealias {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirTypealiasBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildTypealiasCopy(original: SirTypealias, init: SirTypealiasBuilder.() -> Unit): SirTypealias {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirTypealiasBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.documentation = original.documentation
    copyBuilder.name = original.name
    copyBuilder.type = original.type
    return copyBuilder.apply(init).build()
}
