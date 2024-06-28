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
import org.jetbrains.kotlin.sir.impl.SirExtensionImpl

@SirBuilderDsl
class SirExtensionBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    var documentation: String? = null
    val declarations: MutableList<SirDeclaration> = mutableListOf()
    lateinit var extendedType: SirType

    fun build(): SirExtension {
        return SirExtensionImpl(
            origin,
            visibility,
            documentation,
            declarations,
            extendedType,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildExtension(init: SirExtensionBuilder.() -> Unit): SirExtension {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirExtensionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildExtensionCopy(original: SirExtension, init: SirExtensionBuilder.() -> Unit): SirExtension {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirExtensionBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.documentation = original.documentation
    copyBuilder.declarations.addAll(original.declarations)
    copyBuilder.extendedType = original.extendedType
    return copyBuilder.apply(init).build()
}
