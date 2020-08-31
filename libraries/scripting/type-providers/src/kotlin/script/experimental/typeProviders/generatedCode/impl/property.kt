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
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImport
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImports

@PublishedApi
internal fun <T : Any, O : Any> GeneratedCode.Builder.property(
    name: String,
    receiverType: IdentifiableMember,
    isExtension: Boolean,
    type: IdentifiableMember,
    getter: T.() -> O,
    setter: (T.(O) -> Unit)?
) {
    +RuntimeProperty(
        name = name,
        receiverType = receiverType,
        isExtension = isExtension,
        includeType = false,
        getter = getter,
        setter = setter,
        type = type
    )
}

@PublishedApi
internal fun <T : Any> GeneratedCode.Builder.property(
    name: String,
    receiverType: IdentifiableMember,
    isExtension: Boolean,
    type: IdentifiableMember,
    getter: T.(KType) -> Any,
    setter: (T.(Any) -> Unit)?
) {
    +RuntimeProperty(
        name = name,
        receiverType = receiverType,
        isExtension = isExtension,
        includeType = true,
        getter = getter,
        setter = setter,
        type = type
    )
}

inline fun <reified T : Any, reified O : Any> GeneratedCode.Builder.extensionProperty(
    name: String,
    noinline getter: T.() -> O
) {
    property(name, IdentifiableMember<T>(), true, IdentifiableMember<O>(), getter, null)
}

inline fun <reified T : Any, reified O : Any> GeneratedCode.Builder.extensionProperty(
    name: String,
    noinline getter: T.() -> O,
    noinline setter: T.(O) -> Unit
) {
    property(name, IdentifiableMember<T>(), true, IdentifiableMember<O>(), getter, setter)
}

inline fun <reified T : Any> GeneratedCode.Builder.extensionProperty(
    name: String,
    type: IdentifiableMember,
    noinline getter: T.(KType) -> Any
) {
    property(name, IdentifiableMember<T>(), true, type, getter, null)
}

inline fun <reified T : Any> GeneratedCode.Builder.extensionProperty(
    name: String,
    type: IdentifiableMember,
    noinline getter: T.(KType) -> Any,
    noinline setter: T.(Any) -> Unit
) {
    property(name, IdentifiableMember<T>(), true, type, getter, setter)
}

fun GeneratedCode.Builder.extensionProperty(
    name: String,
    receiverType: IdentifiableMember,
    type: IdentifiableMember,
    getter: GeneratedTypeReceiver.(KType) -> Any
) {
    property<Any>(
        name = name,
        receiverType = receiverType,
        isExtension = true,
        type = type,
        getter = { kType ->
            GeneratedTypeReceiver(this, this::class).getter(kType)
        },
        setter = null
    )
}

fun GeneratedCode.Builder.extensionProperty(
    name: String,
    receiverType: IdentifiableMember,
    type: IdentifiableMember,
    getter: GeneratedTypeReceiver.(KType) -> Any,
    setter: GeneratedTypeReceiver.(Any) -> Unit
) {
    property<Any>(
        name = name,
        receiverType = receiverType,
        isExtension = true,
        type = type,
        getter = { kType ->
            GeneratedTypeReceiver(this, this::class).getter(kType)
        },
        setter = { newValue ->
            GeneratedTypeReceiver(this, this::class).setter(newValue)
        }
    )
}


inline fun <reified O : Any> GeneratedCode.Builder.extensionProperty(
    name: String,
    receiverType: IdentifiableMember,
    noinline getter: GeneratedTypeReceiver.() -> O
) {
    property<Any, O>(
        name = name,
        receiverType = receiverType,
        isExtension = true,
        type = IdentifiableMember<O>(),
        getter = {
            GeneratedTypeReceiver(this, this::class).getter()
        },
        setter = null
    )
}

inline fun <reified O : Any> GeneratedCode.Builder.extensionProperty(
    name: String,
    receiverType: IdentifiableMember,
    noinline getter: GeneratedTypeReceiver.() -> O,
    noinline setter: GeneratedTypeReceiver.(O) -> Unit
) {
    property<Any, O>(
        name = name,
        receiverType = receiverType,
        isExtension = true,
        type = IdentifiableMember<O>(),
        getter = {
            GeneratedTypeReceiver(this, this::class).getter()
        },
        setter = { newValue ->
            GeneratedTypeReceiver(this, this::class).setter(newValue)
        }
    )
}


inline fun <reified O : Any> ClassLikeBuilder<*>.property(name: String, noinline getter: GeneratedTypeReceiver.() -> O) {
    property<Any, O>(
        name = name,
        receiverType = IdentifiableMember<Any>(),
        isExtension = false,
        type = IdentifiableMember<O>(),
        getter = {
            GeneratedTypeReceiver(this, this::class).getter()
        },
        setter = null
    )
}

inline fun <reified O : Any> ClassLikeBuilder<*>.property(
    name: String,
    noinline getter: GeneratedTypeReceiver.() -> O,
    noinline setter: GeneratedTypeReceiver.(O) -> Unit
) {
    property<Any, O>(
        name = name,
        receiverType = IdentifiableMember<Any>(),
        isExtension = false,
        type = IdentifiableMember<O>(),
        getter = {
            GeneratedTypeReceiver(this, this::class).getter()
        },
        setter = { newValue ->
            GeneratedTypeReceiver(this, this::class).setter(newValue)
        }
    )
}

fun ClassLikeBuilder<*>.property(
    name: String,
    type: IdentifiableMember,
    getter: GeneratedTypeReceiver.(KType) -> Any,
) {
    property<Any>(
        name = name,
        receiverType = IdentifiableMember<Any>(),
        isExtension = false,
        type = type,
        getter = {
            GeneratedTypeReceiver(this, this::class).getter(it)
        },
        setter = null
    )
}

fun ClassLikeBuilder<*>.property(
    name: String,
    type: IdentifiableMember,
    getter: GeneratedTypeReceiver.(KType) -> Any,
    setter: GeneratedTypeReceiver.(Any) -> Unit
) {
    property<Any>(
        name = name,
        receiverType = IdentifiableMember<Any>(),
        isExtension = false,
        type = type,
        getter = {
            GeneratedTypeReceiver(this, this::class).getter(it)
        },
        setter = { newValue ->
            GeneratedTypeReceiver(this, this::class).setter(newValue)
        }
    )
}

private class RuntimeProperty(
    val name: String,
    val receiverType: IdentifiableMember,
    val isExtension: Boolean,
    val includeType: Boolean,
    val getter: Any,
    val setter: Any?,
    val type: IdentifiableMember
) : InternalGeneratedCode() {

    override val isOverridable: Boolean
        get() = !isExtension

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        useImports(receiverType, type)

        if (includeType) {
            useImports(KType::class)
            useImport("kotlin.reflect.typeOf")
        }

        withSerialized(getter) { getterId ->
            writeScript {
                if (setter != null) {
                    append("var ")
                } else {
                    append("val ")
                }

                if (isExtension) {
                    append("${receiverType.name}.$name", indent)
                } else {
                    append(name, indent)
                }

                appendLine(": ${type.name}")
                appendLine("get() {", indent + 1)

                if (includeType) {
                    appendLine(
                        "val getter: ${receiverType.name}.(KType) -> ${type.name} = __Deserialization__.unsafeReadSerializedValue(\"$getterId\")",
                        indent + 2
                    )
                    appendLine("@OptIn(ExperimentalStdlibApi::class)", indent + 2)
                    appendLine("return getter(typeOf<${type.name}>())", indent + 2)

                } else {
                    appendLine(
                        "val getter: ${receiverType.name}.() -> ${type.name} = __Deserialization__.unsafeReadSerializedValue(\"$getterId\")",
                        indent + 2
                    )

                    appendLine("return getter()", indent + 2)
                }
                appendLine("}", indent + 1)

                if (setter != null) {
                    withSerialized(setter) { setterId ->
                        appendLine("set(newValue) {", indent + 1)
                        appendLine(
                            "val setter: ${receiverType.name}.(${type.name}) -> Unit = __Deserialization__.unsafeReadSerializedValue(\"$setterId\")",
                            indent + 2
                        )

                        appendLine("setter(newValue)", indent + 2)
                        appendLine("}", indent + 1)
                    }
                }
            }
        }
    }
}