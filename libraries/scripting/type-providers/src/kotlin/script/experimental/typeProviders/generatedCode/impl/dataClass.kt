/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import java.io.Serializable
import kotlin.script.experimental.typeProviders.generatedCode.*
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visit
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImports

/**
 * Create a data class
 *
 * @param name Name of your data class
 */
fun GeneratedCode.Builder.dataClass(
    name: String,
    init: DataClassBuilder<*>.() -> Unit
): IdentifiableMember = DataClass.Builder(name).apply(init).build().also { +it }

/**
 * Builder for a provided data class. It can implement interfaces, have code inside and have a number of properties
 */
@GeneratedCodeDSL
interface DataClassBuilder<Overridden : DataClassBuilder<Overridden>> : ClassLikeBuilder<Overridden> {
    fun property(name: String, type: IdentifiableMember, mutable: Boolean = false, default: GeneratedCode? = null)
}

/**
 * Add a property
 */
@PublishedApi
internal fun <T : Serializable> DataClassBuilder<*>.property(name: String, type: IdentifiableMember, mutable: Boolean, default: T?) {
    property(name, type, mutable, default?.let { makeValue(it, type) })
}

/**
 * Add a property
 */
inline fun <reified T : Serializable> DataClassBuilder<*>.property(name: String, mutable: Boolean = false, default: T? = null) {
    property(name, IdentifiableMember<T>(), mutable, default)
}

private class DataClass(
    override val name: String,
    private val properties: Set<Property>,
    private val classLike: ClassLike
) : InternalGeneratedCode(), IdentifiableMember {

    class Property(
        val name: String,
        val type: IdentifiableMember,
        val isOverride: Boolean,
        val isMutable: Boolean,
        val default: GeneratedCode? = null
    ) : InternalGeneratedCode() {

        override fun GeneratedCodeVisitor.visit(indent: Int) {
            useImports(type)

            writeScript {
                if (isOverride) {
                    append("override ", indent)
                } else {
                    append("", indent)
                }

                if (isMutable) {
                    append("var ")
                } else {
                    append("val ")
                }

                append("$name: ${type.name}")
                default?.let { default ->
                    append(" = ")
                    visit(default, indent)
                }
            }
        }

    }

    class Builder(
        name: String,
        private val classLikeBuilder: ClassLike.Builder = ClassLike.Builder(name)
    ) : DataClassBuilder<Builder.OverriddenBuilder> {

        override val name: String
            get() = classLikeBuilder.name

        class OverriddenBuilder(
            val builder: Builder,
            private val overrideBuilder: OverrideBuilder = OverrideBuilder(builder)
        ) : DataClassBuilder<OverriddenBuilder> by builder {
            override fun GeneratedCode.unaryPlus() {
                with(overrideBuilder) { +this@unaryPlus }
            }

            override fun property(name: String, type: IdentifiableMember, mutable: Boolean, default: GeneratedCode?) {
                builder.properties.add(Property(name, type, true, mutable, default))
            }
        }

        private val properties = mutableSetOf<Property>()

        override fun GeneratedCode.unaryPlus() {
            with(classLikeBuilder) {
                +this@unaryPlus
            }
        }

        override fun implement(generated: GeneratedInterface) {
            classLikeBuilder.implement(generated)
        }

        override fun property(name: String, type: IdentifiableMember, mutable: Boolean, default: GeneratedCode?) {
            properties.add(Property(name, type, false, mutable, default))
        }

        override fun override(init: OverriddenBuilder.() -> Unit) {
            OverriddenBuilder(this).init()
        }

        fun build(): DataClass {
            return DataClass(
                name = name,
                properties = properties,
                classLike = classLikeBuilder.build()
            )
        }
    }

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        writeScript {
            appendLine("data class $name(", indent)
            writeJoined(properties, ",\n", indent + 1)
            appendLine()
            append(")", indent)
            visit(classLike, indent)
        }
    }
}
