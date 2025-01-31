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
import org.jetbrains.kotlin.sir.impl.SirInitImpl

@SirBuilderDsl
class SirInitBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    var documentation: String? = null
    val attributes: MutableList<SirAttribute> = mutableListOf()
    var body: SirFunctionBody? = null
    var errorType: SirType = SirType.never
    var isFailable: Boolean by kotlin.properties.Delegates.notNull<Boolean>()
    val parameters: MutableList<SirParameter> = mutableListOf()
    var isConvenience: Boolean = false
    var isRequired: Boolean = false
    var isOverride: Boolean = false

    fun build(): SirInit {
        return SirInitImpl(
            origin,
            visibility,
            documentation,
            attributes,
            body,
            errorType,
            isFailable,
            parameters,
            isConvenience,
            isRequired,
            isOverride,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildInit(init: SirInitBuilder.() -> Unit): SirInit {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirInitBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildInitCopy(original: SirInit, init: SirInitBuilder.() -> Unit): SirInit {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirInitBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.documentation = original.documentation
    copyBuilder.attributes.addAll(original.attributes)
    copyBuilder.body = original.body
    copyBuilder.errorType = original.errorType
    copyBuilder.isFailable = original.isFailable
    copyBuilder.parameters.addAll(original.parameters)
    copyBuilder.isConvenience = original.isConvenience
    copyBuilder.isRequired = original.isRequired
    copyBuilder.isOverride = original.isOverride
    return copyBuilder.apply(init).build()
}
