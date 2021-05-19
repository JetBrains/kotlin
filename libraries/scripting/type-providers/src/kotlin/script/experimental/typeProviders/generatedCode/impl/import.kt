/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.impl

import kotlin.reflect.KClass
import kotlin.reflect.typeOf
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.IdentifiableMember
import kotlin.script.experimental.typeProviders.generatedCode.internal.InternalGeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.GeneratedCodeVisitor
import kotlin.script.experimental.typeProviders.generatedCode.internal.visitor.useImports

inline fun <reified T> GeneratedCode.Builder.import() = import(*IdentifiableMember<T>().imports().toTypedArray())

fun GeneratedCode.Builder.import(vararg imports: String) = +ImportCode(imports.asIterable())

fun GeneratedCode.Builder.import(vararg imports: KClass<*>) = +ImportCode(imports.map { it.qualifiedName!! })

private data class ImportCode(val imports: Iterable<String>) : InternalGeneratedCode() {
    override fun GeneratedCodeVisitor.visit(indent: Int) {
        useImports(imports)
    }
}