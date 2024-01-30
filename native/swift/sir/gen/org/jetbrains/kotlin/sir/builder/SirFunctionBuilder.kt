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
import org.jetbrains.kotlin.sir.impl.SirFunctionImpl

@SirBuilderDsl
class SirFunctionBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    var isStatic: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    lateinit var name: String
    val parameters: MutableList<SirParameter> = mutableListOf()
    lateinit var returnType: SirType
    var body: SirFunctionBody? = null
    var documentation: String? = null

    fun build(): SirFunction {
        return SirFunctionImpl(
            origin,
            visibility,
            isStatic,
            name,
            parameters,
            returnType,
            body,
            documentation,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildFunction(init: SirFunctionBuilder.() -> Unit): SirFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirFunctionBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildFunctionCopy(original: SirFunction, init: SirFunctionBuilder.() -> Unit): SirFunction {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirFunctionBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.isStatic = original.isStatic
    copyBuilder.name = original.name
    copyBuilder.parameters.addAll(original.parameters)
    copyBuilder.returnType = original.returnType
    copyBuilder.body = original.body
    copyBuilder.documentation = original.documentation
    return copyBuilder.apply(init).build()
}
