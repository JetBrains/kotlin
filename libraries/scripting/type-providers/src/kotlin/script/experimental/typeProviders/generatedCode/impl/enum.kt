/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import kotlin.script.experimental.typeProviders.generatedCode.*
import kotlin.script.experimental.typeProviders.generatedCode.ClassLike
import kotlin.script.experimental.typeProviders.generatedCode.StandardBuilder
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visit
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImports

/**
 * Create an enum
 *
 * @param name Name of your data class
 */
fun GeneratedCode.Builder.enum(
    name: String,
    init: EnumBuilder<*>.() -> Unit
): IdentifiableMember = Enum.Builder(name).apply(init).build().also { +it }

interface EnumBuilder<Overridden : ClassLikeBuilder<Overridden>> : ClassLikeBuilder<Overridden> {
    fun case(name: String)
}

private class Enum(
    override val name: String,
    private val cases: Set<String>,
    private val interfaces: Set<GeneratedInterface>,
    private val children: GeneratedCode
) : InternalGeneratedCode(), IdentifiableMember {

    class Builder(
        override val name: String,
        private val childrenBuilder: StandardBuilder = StandardBuilder()
    ) : ClassLikeBuilder<Builder.OverriddenBuilder>, GeneratedCode.Builder by childrenBuilder, EnumBuilder<Builder.OverriddenBuilder> {
        private val cases = mutableSetOf<String>()
        private val interfaces = mutableSetOf<GeneratedInterface>()

        class OverriddenBuilder(
            private val builder: Builder,
            private val overrideBuilder: OverrideBuilder = OverrideBuilder(builder)
        ) : ClassLikeBuilder<OverriddenBuilder> by builder {
            override fun GeneratedCode.unaryPlus() {
                with(overrideBuilder) { +this@unaryPlus }
            }

            override fun override(init: OverriddenBuilder.() -> Unit) {
                init()
            }
        }

        override fun case(name: String) {
            cases.add(name)
        }

        override fun implement(generated: GeneratedInterface) {
            interfaces.add(generated)
        }

        fun build(): Enum {
            return Enum(name, cases, interfaces, childrenBuilder.build())
        }

        override fun override(init: OverriddenBuilder.() -> Unit) {
            OverriddenBuilder(this).init()
        }
    }

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        useImports(interfaces)

        writeScript {
            appendLine("enum class $name", indent)
            if (interfaces.isNotEmpty()) {
                append(" : ")
                appendJoined(interfaces, ", ") { append(it.name) }
            }

            appendLine(" {")
            appendJoined(cases, ", ") { append(it, indent + 1) }
            if (children != GeneratedCode.Empty) {
                appendLine(";")
                visit(children, indent + 1)
            } else {
                appendLine()
            }
            appendLine("}", indent)
        }
    }

}