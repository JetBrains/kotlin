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
import org.jetbrains.kotlin.sir.impl.SirVariableImpl

@SirBuilderDsl
class SirVariableBuilder {
    var origin: SirOrigin = SirOrigin.Unknown
    var visibility: SirVisibility = SirVisibility.PUBLIC
    lateinit var name: String
    lateinit var type: SirType
    lateinit var getter: SirGetter
    var setter: SirSetter? = null
    var isStatic: Boolean by kotlin.properties.Delegates.notNull<Boolean>()

    fun build(): SirVariable {
        return SirVariableImpl(
            origin,
            visibility,
            name,
            type,
            getter,
            setter,
            isStatic,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildVariable(init: SirVariableBuilder.() -> Unit): SirVariable {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return SirVariableBuilder().apply(init).build()
}

@OptIn(ExperimentalContracts::class)
inline fun buildVariableCopy(original: SirVariable, init: SirVariableBuilder.() -> Unit): SirVariable {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val copyBuilder = SirVariableBuilder()
    copyBuilder.origin = original.origin
    copyBuilder.visibility = original.visibility
    copyBuilder.name = original.name
    copyBuilder.type = original.type
    copyBuilder.getter = original.getter
    copyBuilder.setter = original.setter
    copyBuilder.isStatic = original.isStatic
    return copyBuilder.apply(init).build()
}
