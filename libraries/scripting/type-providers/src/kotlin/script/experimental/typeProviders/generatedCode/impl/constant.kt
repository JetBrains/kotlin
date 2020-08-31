/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import java.io.Serializable
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.IdentifiableMember
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visit
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImports

/**
 * Create a constant with a value
 *
 * @param name Name of your constant
 * @param value Generated Code representing the value of the constant
 * @param type Generated Code representing the type. Set to null if type inference can figure it out
 */
fun GeneratedCode.Builder.constant(
    name: String,
    value: GeneratedCode,
    type: IdentifiableMember? = null
) = +Constant(name, value, type)

@PublishedApi
internal fun <T : Serializable> GeneratedCode.Builder.constant(
    name: String,
    value: T,
    type: IdentifiableMember
) = constant(
    name,
    makeValue(value, type),
    type
)

/**
 * Create a constant with a value
 *
 * @param name Name of your constant
 * @param value Generated Code representing the value of the constant
 */
inline fun <reified T : Serializable> GeneratedCode.Builder.constant(
    name: String,
    value: T
) = constant(name, value, IdentifiableMember<T>())

internal class Constant(
    private val name: String,
    private val value: GeneratedCode,
    private val type: IdentifiableMember? = null
) : InternalGeneratedCode() {

    override val isOverridable: Boolean
        get() = true

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        type?.let { useImports(it) }
        writeScript {
            append("val $name", indent)
            type?.let { type ->
                append(": ")
                append(type.name)
            }
            append(" = ")
            visit(value, indent)
            appendLine()
        }
    }
}