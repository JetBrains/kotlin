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
import org.jetbrains.kotlin.sir.impl.SirGetterImpl

@SirBuilderDsl
class SirGetterBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    var body: SirFunctionBody? = null

    fun build(): SirGetter {
        return SirGetterImpl(
            origin,
            visibility,
            body,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildGetter(init: SirGetterBuilder.() -> Unit = {}): SirGetter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirGetterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildGetterCopy(original: SirGetter, init: SirGetterBuilder.() -> Unit = {}): SirGetter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirGetterBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.body = original.body
    return copyBuilder.apply(init).build()
}
