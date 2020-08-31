/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import kotlin.script.experimental.typeProviders.generatedCode.*
import kotlin.script.experimental.typeProviders.generatedCode.impl.Object.Kind.*
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visit

/**
 * Generate an object with a name
 *
 * @param name name of the object
 * @param init initialize using an ObjectBuilder
 */
fun GeneratedCode.Builder.namedObject(
    name: String,
    init: ClassLikeBuilder<*>.() -> Unit
): IdentifiableMember = `object`(Named(name), init)

/**
 * Add a companion object to the generated type
 */
fun ClassLikeBuilder<*>.companionObject(
    name: String? = null,
    init: ClassLikeBuilder<*>.() -> Unit
): IdentifiableMember = `object`(Companion(name, this), init)

private fun GeneratedCode.Builder.`object`(
    kind: Object.Kind,
    init: ClassLikeBuilder<*>.() -> Unit
): IdentifiableMember {
    val builder = ClassLike.Builder(kind.name).apply(init)
    return Object(
        kind,
        builder.name,
        builder.build()
    ).also { +it }
}

private class Object(
    private val kind: Kind,
    override val name: String,
    private val classLike: ClassLike
) : InternalGeneratedCode(), IdentifiableMember {
    sealed class Kind {
        abstract val name: String

        class Named(override val name: String) : Kind()

        class Companion(val objectName: String?, val parent: IdentifiableMember) : Kind() {
            override val name: String
                get() = objectName ?: "${parent.name}.Companion"
        }
    }

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        writeScript {
            when (kind) {
                is Named -> append("object $name", indent)
                is Companion -> {
                    append("companion object", indent)
                    if (kind.objectName != null) {
                        append(" ${kind.objectName}")
                    }
                }
            }

            visit(classLike, indent)
        }
    }
}