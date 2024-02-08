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
import org.jetbrains.kotlin.sir.SirImport
import org.jetbrains.kotlin.sir.SirOrigin
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.impl.SirImportImpl

@SirBuilderDsl
class SirImportBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    lateinit var moduleName: String

    fun build(): SirImport {
        return SirImportImpl(
            origin,
            visibility,
            moduleName,
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildImport(init: SirImportBuilder.() -> Unit): SirImport {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirImportBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildImportCopy(original: SirImport, init: SirImportBuilder.() -> Unit): SirImport {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirImportBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.moduleName = original.moduleName
    return copyBuilder.apply(init).build()
}
