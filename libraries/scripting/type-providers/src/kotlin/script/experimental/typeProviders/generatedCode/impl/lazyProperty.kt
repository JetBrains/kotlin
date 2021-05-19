/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import kotlin.reflect.KType
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.ClassLikeBuilder
import kotlin.script.experimental.typeProviders.generatedCode.IdentifiableMember
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImports

@PublishedApi
internal fun <O : Any> GeneratedCode.Builder.lazyProperty(
    name: String,
    type: IdentifiableMember,
    init: () -> O,
) {
    +RuntimeLazyProperty(
        name = name,
        withReceiver = false,
        includeTypeInCall = false,
        init = init,
        type = type
    )
}

@PublishedApi
internal fun <T : Any, O : Any> GeneratedCode.Builder.lazyPropertyWithReceiver(
    name: String,
    type: IdentifiableMember,
    init: T.() -> O,
) {
    +RuntimeLazyProperty(
        name = name,
        withReceiver = true,
        includeTypeInCall = false,
        init = init,
        type = type
    )
}

fun GeneratedCode.Builder.lazyProperty(
    name: String,
    type: IdentifiableMember,
    init: (KType) -> Any,
) {
    +RuntimeLazyProperty(
        name = name,
        withReceiver = false,
        includeTypeInCall = true,
        init = init,
        type = type
    )
}

fun <T : Any> GeneratedCode.Builder.lazyPropertyWithReceiver(
    name: String,
    type: IdentifiableMember,
    init: T.(KType) -> Any,
) {
    +RuntimeLazyProperty(
        name = name,
        withReceiver = true,
        includeTypeInCall = true,
        init = init,
        type = type
    )
}

inline fun <reified O : Any> GeneratedCode.Builder.lazyProperty(
    name: String,
    noinline init: () -> O,
) = lazyProperty(name, IdentifiableMember<O>(), init)

inline fun <reified O : Any> ClassLikeBuilder<*>.lazyPropertyWithReceiver(name: String, noinline init: GeneratedTypeReceiver.() -> O) {
    lazyPropertyWithReceiver<Any, O>(
        name = name,
        type = IdentifiableMember<O>(),
        init = {
            GeneratedTypeReceiver(this, this::class).init()
        }
    )
}

fun ClassLikeBuilder<*>.lazyPropertyWithReceiver(
    name: String,
    type: IdentifiableMember,
    init: GeneratedTypeReceiver.(KType) -> Any,
) {
    lazyPropertyWithReceiver(
        name = name,
        type = type,
        init = {
            GeneratedTypeReceiver(this, this::class).init(it)
        }
    )
}

private class RuntimeLazyProperty(
    val name: String,
    val withReceiver: Boolean,
    val includeTypeInCall: Boolean,
    val init: Any,
    val type: IdentifiableMember
) : InternalGeneratedCode() {

    override val isOverridable: Boolean
        get() = true

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        useImports(type)

        if (includeTypeInCall) {
            useImports(KType::class)
            useImport("kotlin.reflect.typeOf")
        }

        withSerialized(init) { id ->
            writeScript {
                appendLine("val $name: ${type.name} by lazy {", indent)
                append("val function: ", indent + 1)

                when (withReceiver to includeTypeInCall) {
                    false to false -> append("() -> ${type.name}")
                    false to true -> append("(KType) -> ${type.name}")
                    true to false -> append("Any.() -> ${type.name}")
                    true to true -> append("Any.(KType) -> ${type.name}")
                    else -> Unit
                }

                appendLine(" = __Deserialization__.unsafeReadSerializedValue(\"$id\")")

                if (includeTypeInCall) {
                    appendLine("@OptIn(ExperimentalStdlibApi::class)", indent + 1)
                    appendLine("function(typeOf<${type.name}>())", indent + 1)
                } else {
                    appendLine("function()", indent + 1)
                }

                appendLine("}", indent)
            }
        }
    }
}