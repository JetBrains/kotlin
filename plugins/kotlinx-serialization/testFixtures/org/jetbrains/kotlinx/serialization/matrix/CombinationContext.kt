/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.matrix


interface CombinationContext {
    fun defineEnums(
        serializers: Set<SerializerKind>,
        locations: Set<TypeLocation>,
        optionsConfig: EnumOptionsBuilder.() -> Unit = {},
    ): Set<EnumVariant>

    fun function(name: String, block: FunctionContext.() -> Unit)

    fun box(block: FunctionContext.() -> Unit)

    fun generate(appendable: Appendable, generator: String)

    val TypeVariant.named: NamedTypeVariant
}


interface FunctionContext {
    fun line(code: String = "")
}

interface EnumOptionsBuilder {
    fun serialInfo(vararg serialInfo: SerialInfo)
    fun descriptorAccessing(vararg descriptorAccessing: DescriptorAccessing)

    fun entries(vararg entries: String)
}


