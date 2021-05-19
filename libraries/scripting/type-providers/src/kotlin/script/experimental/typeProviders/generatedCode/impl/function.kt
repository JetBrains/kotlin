/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import kotlin.reflect.KClass
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.IdentifiableMember
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImport
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImports

@PublishedApi
internal fun <T : Any> GeneratedCode.Builder.function(name: String, returnType: IdentifiableMember, code: () -> T) {
    +RuntimeFunctionCode(name, code, emptyMap(), returnType)
}

@PublishedApi
internal fun <T : Any, A : Any> GeneratedCode.Builder.function(
    name: String,
    arg0Name: String,
    arg0Type: IdentifiableMember,
    returnType: IdentifiableMember,
    code: (A) -> T
) {
    +RuntimeFunctionCode(name, code, mapOf(arg0Name to arg0Type), returnType)
}

@PublishedApi
internal fun <T : Any, A : Any, B : Any> GeneratedCode.Builder.function(
    name: String,
    arg0Name: String,
    arg0Type: IdentifiableMember,
    arg1Name: String,
    arg1Type: IdentifiableMember,
    returnType: IdentifiableMember,
    code: (A, B) -> T
) {
    +RuntimeFunctionCode(
        name,
        code,
        mapOf(arg0Name to arg0Type, arg1Name to arg1Type),
        returnType
    )
}

@PublishedApi
internal fun <T : Any, A : Any, B : Any, C : Any> GeneratedCode.Builder.function(
    name: String,
    arg0Name: String,
    arg0Type: IdentifiableMember,
    arg1Name: String,
    arg1Type: IdentifiableMember,
    arg2Name: String,
    arg2Type: IdentifiableMember,
    returnType: IdentifiableMember,
    code: (A, B, C) -> T
) {
    +RuntimeFunctionCode(
        name,
        code,
        mapOf(arg0Name to arg0Type, arg1Name to arg1Type, arg2Name to arg2Type),
        returnType
    )
}

inline fun <reified T : Any> GeneratedCode.Builder.function(name: String, noinline code: () -> T) = function(name, IdentifiableMember<T>(), code)

inline fun <reified T : Any, reified A : Any> GeneratedCode.Builder.function(
    name: String,
    arg0Name: String = "arg0",
    noinline code: (A) -> T
) {
    function(name, arg0Name, IdentifiableMember<A>(), IdentifiableMember<T>(), code)
}

inline fun <reified T : Any, reified A : Any, reified B : Any> GeneratedCode.Builder.function(
    name: String,
    arg0Name: String = "arg0",
    arg1Name: String = "arg1",
    noinline code: (A, B) -> T
) {
    function(name, arg0Name, IdentifiableMember<A>(), arg1Name, IdentifiableMember<B>(), IdentifiableMember<T>(), code)
}

inline fun <reified T : Any, reified A : Any, reified B : Any, reified C : Any> GeneratedCode.Builder.function(
    name: String,
    arg0Name: String = "arg0",
    arg1Name: String = "arg1",
    arg2Name: String = "arg2",
    noinline code: (A, B, C) -> T
) {
    function(name, arg0Name, IdentifiableMember<A>(), arg1Name, IdentifiableMember<B>(), arg2Name, IdentifiableMember<C>(), IdentifiableMember<T>(), code)
}

private class RuntimeFunctionCode(
    private val name: String,
    private val function: Any,
    private val args: Map<String, IdentifiableMember>,
    private val returnType: IdentifiableMember
) : InternalGeneratedCode() {

    override val isOverridable: Boolean
        get() = true

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        useImports(returnType)
        useImports(args.values)

        withSerialized(function) { id ->
            writeScript {
                // Open
                append("fun $name(", indent)
                appendJoined(args.asIterable(), ", ") { (name, type) ->
                    append("$name: ${type.name}")
                }
                appendLine("): ${returnType.name} {")

                // Load function
                appendLine("val function: (", indent + 1)
                appendJoined(args.values, ", ") { type ->
                    append(type.name)
                }
                append(") -> ${returnType.name}")
                appendLine(" = __Deserialization__.unsafeReadSerializedValue(\"$id\")")

                // Call and return
                append("return function(", indent + 1)
                appendJoined(args.keys, ", ") { append(it) }
                appendLine(")")

                // Close
                appendLine("}", indent)
            }
        }
    }
}