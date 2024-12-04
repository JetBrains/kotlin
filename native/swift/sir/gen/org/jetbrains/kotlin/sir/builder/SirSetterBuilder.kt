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
import org.jetbrains.kotlin.sir.impl.SirSetterImpl

@SirBuilderDsl
class SirSetterBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    var documentation: String? = null
    val attributes: MutableList<SirAttribute> = mutableListOf()
    var body: SirFunctionBody? = null
    var errorType: SirType = SirType.never
    var parameterName: String = "newValue"

    fun build(): SirSetter {
        return SirSetterImpl(
            origin,
            visibility,
            documentation,
            attributes,
            body,
            errorType,
            parameterName,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildSetter(init: SirSetterBuilder.() -> Unit = {}): SirSetter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirSetterBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildSetterCopy(original: SirSetter, init: SirSetterBuilder.() -> Unit = {}): SirSetter {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirSetterBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.documentation = original.documentation
    copyBuilder.attributes.addAll(original.attributes)
    copyBuilder.body = original.body
    copyBuilder.errorType = original.errorType
    copyBuilder.parameterName = original.parameterName
    return copyBuilder.apply(init).build()
}
