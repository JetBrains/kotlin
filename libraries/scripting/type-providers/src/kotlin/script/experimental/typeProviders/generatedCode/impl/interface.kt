/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import kotlin.script.experimental.typeProviders.generatedCode.*
import kotlin.script.experimental.typeProviders.generatedCode.ClassLike
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visit
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImports

fun GeneratedCode.Builder.`interface`(
    name: String,
    init: InterfaceBuilder<*>.() -> Unit = {}
): GeneratedInterface {
    return Interface.Builder(name).apply(init).build().also { +it }
}

/**
 * References an interface that can be implemented
 */
interface GeneratedInterface : IdentifiableMember

@GeneratedCodeDSL
interface InterfaceBuilder<Overridden : ClassLikeBuilder<Overridden>> : ClassLikeBuilder<Overridden> {
    fun property(name: String, type: IdentifiableMember, mutable: Boolean = false)

    fun method(name: String, returnType: IdentifiableMember)

    fun method(name: String, arg0Name: String = "arg0", arg0Type: IdentifiableMember, returnType: IdentifiableMember)

    fun method(
        name: String,
        arg0Name: String = "arg0",
        arg0Type: IdentifiableMember,
        arg1Name: String = "arg1",
        arg1Type: IdentifiableMember,
        returnType: IdentifiableMember
    )

    fun method(
        name: String,
        arg0Name: String = "arg0",
        arg0Type: IdentifiableMember,
        arg1Name: String = "arg1",
        arg1Type: IdentifiableMember,
        arg2Name: String = "arg2",
        arg2Type: IdentifiableMember,
        returnType: IdentifiableMember
    )
}

inline fun <reified T> InterfaceBuilder<*>.property(name: String, mutable: Boolean = false) {
    property(name, IdentifiableMember<T>(), mutable)
}

inline fun <reified O> InterfaceBuilder<*>.method(
    name: String
) {
    method(name, IdentifiableMember<O>())
}

inline fun <reified A, reified O> InterfaceBuilder<*>.method(
    name: String,
    arg0Name: String = "arg0"
) {
    method(name, arg0Name, IdentifiableMember<A>(), IdentifiableMember<O>())
}

inline fun <reified A, reified B, reified O> InterfaceBuilder<*>.method(
    name: String,
    arg0Name: String = "arg0",
    arg1Name: String = "arg1"
) {
    method(name, arg0Name, IdentifiableMember<A>(), arg1Name, IdentifiableMember<B>(), IdentifiableMember<O>())
}

inline fun <reified A, reified B, reified C, reified O> InterfaceBuilder<*>.method(
    name: String,
    arg0Name: String = "arg0",
    arg1Name: String = "arg1",
    arg2Name: String = "arg2"
) {
    method(
        name,
        arg0Name,
        IdentifiableMember<A>(),
        arg1Name,
        IdentifiableMember<B>(),
        arg2Name,
        IdentifiableMember<C>(),
        IdentifiableMember<O>()
    )
}

private class Interface(
    override val name: String,
    private val classLike: ClassLike
) : InternalGeneratedCode(), GeneratedInterface {

    private class Property(
        val name: String,
        val type: IdentifiableMember,
        val mutable: Boolean
    ) : InternalGeneratedCode() {
        override fun GeneratedCodeVisitor.visit(indent: Int) {
            useImports(type)

            writeScript {
                if (mutable) {
                    append("var ", indent)
                } else {
                    append("val ", indent)
                }
                append("$name: ${type.name}")
            }
        }
    }

    private class Method(
        val name: String,
        val args: Map<String, IdentifiableMember>,
        val returnType: IdentifiableMember,
    ) : InternalGeneratedCode() {
        override fun GeneratedCodeVisitor.visit(indent: Int) {
            useImports(returnType)
            useImports(args.values)

            writeScript {
                append("fun $name(", indent)
                appendJoined(args.asIterable(), ", ") { (name, type) ->
                    append("$name: ${type.name}")
                }
                appendLine("): ${returnType.name}")
            }
        }
    }

    class Builder(
        name: String,
        private val classLikeBuilder: ClassLike.Builder = ClassLike.Builder(name)
    ) : InterfaceBuilder<ClassLike.Builder.OverriddenBuilder>, ClassLikeBuilder<ClassLike.Builder.OverriddenBuilder> by classLikeBuilder {
        override fun property(name: String, type: IdentifiableMember, mutable: Boolean) {
            +Property(name, type, mutable)
        }

        override fun method(name: String, returnType: IdentifiableMember) {
            +Method(name, emptyMap(), returnType)
        }

        override fun method(name: String, arg0Name: String, arg0Type: IdentifiableMember, returnType: IdentifiableMember) {
            +Method(name, mapOf(arg0Name to arg0Type), returnType)
        }

        override fun method(
            name: String,
            arg0Name: String,
            arg0Type: IdentifiableMember,
            arg1Name: String,
            arg1Type: IdentifiableMember,
            returnType: IdentifiableMember
        ) {
            +Method(name, mapOf(arg0Name to arg0Type, arg1Name to arg1Type), returnType)
        }

        override fun method(
            name: String,
            arg0Name: String,
            arg0Type: IdentifiableMember,
            arg1Name: String,
            arg1Type: IdentifiableMember,
            arg2Name: String,
            arg2Type: IdentifiableMember,
            returnType: IdentifiableMember
        ) {
            +Method(name, mapOf(arg0Name to arg0Type, arg1Name to arg1Type, arg2Name to arg2Type), returnType)
        }

        fun build(): Interface {
            return Interface(name, classLikeBuilder.build())
        }
    }

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        writeScript {
            append("interface $name", indent)

            visit(classLike, indent)
        }
    }
}