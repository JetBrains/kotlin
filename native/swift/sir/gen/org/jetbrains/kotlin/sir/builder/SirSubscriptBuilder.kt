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
import org.jetbrains.kotlin.sir.impl.SirSubscriptImpl

@SirBuilderDsl
class SirSubscriptBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    var documentation: String? = null
    val attributes: MutableList<SirAttribute> = mutableListOf()
    var isOverride: Boolean = false
    var isInstance: Boolean = true
    var modality: SirModality = SirModality.UNSPECIFIED
    val parameters: MutableList<SirParameter> = mutableListOf()
    lateinit var returnType: SirType
    lateinit var getter: SirGetter
    var setter: SirSetter? = null

    fun build(): SirSubscript {
        return SirSubscriptImpl(
            origin,
            visibility,
            documentation,
            attributes,
            isOverride,
            isInstance,
            modality,
            parameters,
            returnType,
            getter,
            setter,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSubscript(init: SirSubscriptBuilder.() -> Unit): SirSubscript {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirSubscriptBuilder().apply(init).build()
}
