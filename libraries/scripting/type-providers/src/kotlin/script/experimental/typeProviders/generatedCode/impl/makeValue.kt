/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import java.io.Serializable
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.IdentifiableMember
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImports

internal fun <T : Serializable> makeValue(value: T, type: IdentifiableMember): GeneratedCode = RuntimeValueCode(value, type)

internal inline fun <reified T : Serializable> makeValue(value: T): GeneratedCode = makeValue(value, IdentifiableMember<T>())

private class RuntimeValueCode(val value: Any, val type: IdentifiableMember) : InternalGeneratedCode() {
    override val isOverridable: Boolean
        get() = true

    override fun GeneratedCodeVisitor.visit(indent: Int) {
        useImports(type)

        withSerialized(value) { id ->
            writeScript {
                append("__Deserialization__.unsafeReadSerializedValue<${type.name}>(\"$id\")", indent)
            }
        }
    }
}
