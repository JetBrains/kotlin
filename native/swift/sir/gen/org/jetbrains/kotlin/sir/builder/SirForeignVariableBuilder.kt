/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.sir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.sir.SirBuilderDsl
import org.jetbrains.kotlin.sir.SirForeignVariable
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.impl.SirForeignVariableImpl

@SirBuilderDsl
class SirForeignVariableBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC

    fun build(): SirForeignVariable {
        return SirForeignVariableImpl(
            origin,
            visibility,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildForeignVariable(init: SirForeignVariableBuilder.() -> Unit = {}): SirForeignVariable {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirForeignVariableBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildForeignVariableCopy(original: SirForeignVariable, init: SirForeignVariableBuilder.() -> Unit = {}): SirForeignVariable {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirForeignVariableBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    return copyBuilder.apply(init).build()
}
