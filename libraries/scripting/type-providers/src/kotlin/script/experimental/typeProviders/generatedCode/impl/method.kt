/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.ClassLikeBuilder
import kotlin.script.experimental.typeProviders.generatedCode.IdentifiableMember
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImports
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode

@PublishedApi
internal fun <T, O> GeneratedCode.Builder.method(
    name: String,
    receiverType: IdentifiableMember,
    isExtension: Boolean,
    returnType: IdentifiableMember,
    code: T.() -> O
) {
    +RuntimeMethodCode(
        name,
        receiverType,
        isExtension,
        code,
        emptyMap(),
        returnType
    )
}

@PublishedApi
internal fun <T, A, O> GeneratedCode.Builder.method(
    name: String,
    receiverType: IdentifiableMember,
    isExtension: Boolean,
    arg0Name: String,
    arg0Type: IdentifiableMember,
    returnType: IdentifiableMember,
    code: T.(A) -> O
) {
    +RuntimeMethodCode(
        name,
        receiverType,
        isExtension,
        code,
        mapOf(arg0Name to arg0Type),
        returnType
    )
}

@PublishedApi
internal fun <T, A, B, O> GeneratedCode.Builder.method(
    name: String,
    receiverType: IdentifiableMember,
    isExtension: Boolean,
    arg0Name: String,
    arg0Type: IdentifiableMember,
    arg1Name: String,
    arg1Type: IdentifiableMember,
    returnType: IdentifiableMember,
    code: T.(A, B) -> O
) {
    +RuntimeMethodCode(
        name,
        receiverType,
        isExtension,
        code,
        mapOf(arg0Name to arg0Type, arg1Name to arg1Type),
        returnType
    )
}

@PublishedApi
internal fun <T, A, B, C, O> GeneratedCode.Builder.method(
    name: String,
    receiverType: IdentifiableMember,
    isExtension: Boolean,
    arg0Name: String,
    arg0Type: IdentifiableMember,
    arg1Name: String,
    arg1Type: IdentifiableMember,
    arg2Name: String,
    arg2Type: IdentifiableMember,
    returnType: IdentifiableMember,
    code: T.(A, B, C) -> O
) {
    +RuntimeMethodCode(
        name,
        receiverType,
        isExtension,
        code,
        mapOf(arg0Name to arg0Type, arg1Name to arg1Type, arg2Name to arg2Type),
        returnType
    )
}

inline fun <reified T, reified O> GeneratedCode.Builder.extensionMethod(
    name: String,
    noinline code: T.() -> O
) {
    method(name, IdentifiableMember<T>(), true, IdentifiableMember<O>(), code)
}

inline fun <reified T, reified A, reified O> GeneratedCode.Builder.extensionMethod(
    name: String,
    arg0Name: String = "arg0",
    noinline code: T.(A) -> O
) {
    method(name, IdentifiableMember<T>(), true, arg0Name, IdentifiableMember<A>(), IdentifiableMember<O>(), code)
}

inline fun <reified T, reified A, reified B, reified O> GeneratedCode.Builder.extensionMethod(
    name: String,
    arg0Name: String = "arg0",
    arg1Name: String = "arg1",
    noinline code: T.(A, B) -> O
) {
    method(
        name,
        IdentifiableMember<T>(),
        true,
        arg0Name,
        IdentifiableMember<A>(),
        arg1Name,
        IdentifiableMember<B>(),
        IdentifiableMember<O>(),
        code
    )
}

inline fun <reified T, reified A, reified B, reified C, reified O> GeneratedCode.Builder.extensionMethod(
    name: String,
    arg0Name: String = "arg0",
    arg1Name: String = "arg1",
    arg2Name: String = "arg2",
    noinline code: T.(A, B, C) -> O
) {
    method(
        name,
        IdentifiableMember<T>(),
        true,
        arg0Name,
        IdentifiableMember<A>(),
        arg1Name,
        IdentifiableMember<B>(),
        arg2Name,
        IdentifiableMember<C>(),
        IdentifiableMember<O>(),
        code
    )
}

inline fun <reified O> GeneratedCode.Builder.extensionMethod(
    name: String,
    receiverType: IdentifiableMember,
    noinline code: GeneratedTypeReceiver.() -> O
) {
    method<Any, O>(name, receiverType, isExtension = true, IdentifiableMember<O>()) {
        GeneratedTypeReceiver(this, this::class).code()
    }
}

inline fun <reified A, reified O> GeneratedCode.Builder.extensionMethod(
    name: String,
    receiverType: IdentifiableMember,
    arg0Name: String = "arg0",
    noinline code: GeneratedTypeReceiver.(A) -> O
) {
    method<Any, A, O>(name, receiverType, true, arg0Name, IdentifiableMember<A>(), IdentifiableMember<O>()) { arg0 ->
        GeneratedTypeReceiver(this, this::class).code(arg0)
    }
}

inline fun <reified A, reified B, reified O> GeneratedCode.Builder.extensionMethod(
    name: String,
    receiverType: IdentifiableMember,
    arg0Name: String = "arg0",
    arg1Name: String = "arg1",
    noinline code: GeneratedTypeReceiver.(A, B) -> O
) {
    method<Any, A, B, O>(
        name,
        receiverType,
        true,
        arg0Name,
        IdentifiableMember<A>(),
        arg1Name,
        IdentifiableMember<B>(),
        IdentifiableMember<O>()
    ) { arg0, arg1 ->
        GeneratedTypeReceiver(this, this::class).code(arg0, arg1)
    }
}

inline fun <reified A, reified B, reified C, reified O> GeneratedCode.Builder.extensionMethod(
    name: String,
    receiverType: IdentifiableMember,
    arg0Name: String = "arg0",
    arg1Name: String = "arg1",
    arg2Name: String = "arg2",
    noinline code: GeneratedTypeReceiver.(A, B, C) -> O
) {
    method<Any, A, B, C, O>(
        name,
        receiverType,
        true,
        arg0Name,
        IdentifiableMember<A>(),
        arg1Name,
        IdentifiableMember<B>(),
        arg2Name,
        IdentifiableMember<C>(),
        IdentifiableMember<O>()
    ) { arg0, arg1, arg2 ->
        GeneratedTypeReceiver(this, this::class).code(arg0, arg1, arg2)
    }
}

inline fun <reified O> ClassLikeBuilder<*>.method(name: String, noinline code: GeneratedTypeReceiver.() -> O) {
    method<Any, O>(name, this, isExtension = false, IdentifiableMember<O>()) {
        GeneratedTypeReceiver(this, this::class).code()
    }
}

inline fun <reified A, reified O> ClassLikeBuilder<*>.method(
    name: String,
    arg0Name: String = "arg0",
    noinline code: GeneratedTypeReceiver.(A) -> O
) {
    method<Any, A, O>(name, this, false, arg0Name, IdentifiableMember<A>(), IdentifiableMember<O>()) { arg0 ->
        GeneratedTypeReceiver(this, this::class).code(arg0)
    }
}

inline fun <reified A, reified B, reified O> ClassLikeBuilder<*>.method(
    name: String,
    arg0Name: String = "arg0",
    arg1Name: String = "arg1",
    noinline code: GeneratedTypeReceiver.(A, B) -> O
) {
    method<Any, A, B, O>(
        name,
        this,
        false,
        arg0Name,
        IdentifiableMember<A>(),
        arg1Name,
        IdentifiableMember<B>(),
        IdentifiableMember<O>()
    ) { arg0, arg1 ->
        GeneratedTypeReceiver(this, this::class).code(arg0, arg1)
    }
}

inline fun <reified A, reified B, reified C, reified O> ClassLikeBuilder<*>.method(
    name: String,
    arg0Name: String = "arg0",
    arg1Name: String = "arg1",
    arg2Name: String = "arg2",
    noinline code: GeneratedTypeReceiver.(A, B, C) -> O
) {
    method<Any, A, B, C, O>(
        name,
        this,
        true,
        arg0Name,
        IdentifiableMember<A>(),
        arg1Name,
        IdentifiableMember<B>(),
        arg2Name,
        IdentifiableMember<C>(),
        IdentifiableMember<O>()
    ) { arg0, arg1, arg2 ->
        GeneratedTypeReceiver(this, this::class).code(arg0, arg1, arg2)
    }
}

private class RuntimeMethodCode(
    val name: String,
    val receiverType: IdentifiableMember,
    val isExtension: Boolean,
    val function: Any,
    val args: Map<String, IdentifiableMember>,
    val returnType: IdentifiableMember
) : InternalGeneratedCode() {

    override val isOverridable: Boolean
        get() = true

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        useImports(receiverType, returnType)
        useImports(args.values)

        withSerialized(function) { id ->
            writeScript {
                // Open
                if (isExtension) {
                    append("fun ${receiverType.name}.$name(", indent)
                } else {
                    append("fun $name(", indent)
                }
                appendJoined(args.asIterable(), ", ") { (name, type) ->
                    append("$name: ${type.name}")
                }
                appendLine("): ${returnType.name} {")

                // Load function
                appendLine("val function: ${receiverType.name}.(", indent + 1)
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