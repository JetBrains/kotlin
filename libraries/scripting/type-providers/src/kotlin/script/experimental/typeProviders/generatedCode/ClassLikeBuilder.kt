/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode

import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf
import kotlin.script.experimental.typeProviders.generatedCode.impl.*
import kotlin.script.experimental.typeProviders.generatedCode.impl.appendJoined
import kotlin.script.experimental.typeProviders.generatedCode.impl.appendLine
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visit
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImports

/**
 * Builder for a provided type. It can implement an interface and have some code inside.
 */
@GeneratedCodeDSL
interface ClassLikeBuilder<Overridden : ClassLikeBuilder<Overridden>> : GeneratedCode.Builder, IdentifiableMember, OverridableBuilder<Overridden> {
    /**
     * Implement a generated interface
     */
    fun implement(generated: GeneratedInterface)
}

/**
 * Implement the interface.
 */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> ClassLikeBuilder<*>.implement() {
    implement(typeOf<T>())
}

@PublishedApi
internal fun ClassLikeBuilder<*>.implement(kType: KType) {
    require(kType.jvmErasure.java.isInterface) { "Provided type is not an interface" }
    implement(IdentifiableMember(kType).asInterface())
}

/**
 * Implement the interface.
 */
fun ClassLikeBuilder<*>.implement(name: String) {
    implement(IdentifiableMember(name).asInterface())
}

/**
 * Implement the interface and specify override methods and properties
 */
fun <O : ClassLikeBuilder<O>> ClassLikeBuilder<O>.implement(generated: GeneratedInterface, init: O.() -> Unit) {
    implement(generated)
    override(init)
}

/**
 * Implement the interface and specify override methods and properties
 */
fun <O : ClassLikeBuilder<O>> ClassLikeBuilder<O>.implement(name: String, init: O.() -> Unit) {
    implement(name)
    override(init)
}

/**
 * Implement the interface and specify override methods and properties
 */
inline fun <O : ClassLikeBuilder<O>, reified T> ClassLikeBuilder<O>.implement(noinline init: O.() -> Unit) {
    implement<T>()
    override(init)
}

/**
 * Implement the interface and specify override methods and properties
 */
fun <O : ClassLikeBuilder<O>> ClassLikeBuilder<O>.implement(kType: KType, init: O.() -> Unit) {
    implement(kType)
    override(init)
}

internal class ClassLike(
    private val interfaces: Set<GeneratedInterface>,
    private val children: GeneratedCode
) : InternalGeneratedCode() {

    internal class Builder(
        override val name: String,
        private val childrenBuilder: StandardBuilder = StandardBuilder()
    ) : ClassLikeBuilder<Builder.OverriddenBuilder>, GeneratedCode.Builder by childrenBuilder {
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

        override fun implement(generated: GeneratedInterface) {
            interfaces.add(generated)
        }

        fun build(): ClassLike {
            return ClassLike(interfaces, childrenBuilder.build())
        }

        override fun override(init: OverriddenBuilder.() -> Unit) {
            OverriddenBuilder(this).init()
        }
    }

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        useImports(interfaces)
        writeScript {
            if (interfaces.isNotEmpty()) {
                append(" : ")
                appendJoined(interfaces, ", ") { append(it.name) }
            }

            if (children != GeneratedCode.Empty) {
                appendLine(" {")
                visit(children, indent + 1)
                appendLine("}", indent)
            } else {
                appendLine()
            }
        }
    }
}