/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/kir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.kir.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.kir.*
import org.jetbrains.kotlin.kir.impl.KirClassImpl

@KirBuilderDsl
class KirClassBuilder {
    var origin: KirOrigin = KirOrigin.Unknown
    var visibility: KirVisibility = KirVisibility.PUBLIC
    var documentation: String? = null
    lateinit var name: String
    val declarations: MutableList<KirDeclaration> = []
    val bridges: MutableList<KirBridge> = []
    var modality: KirModality = KirModality.UNSPECIFIED

    fun build(): KirClass {
        return KirClassImpl(
            origin,
            visibility,
            documentation,
            name,
            declarations,
            bridges,
            modality,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildClass(init: KirClassBuilder.() -> Unit): KirClass {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KirClassBuilder().apply(init).build()
}
