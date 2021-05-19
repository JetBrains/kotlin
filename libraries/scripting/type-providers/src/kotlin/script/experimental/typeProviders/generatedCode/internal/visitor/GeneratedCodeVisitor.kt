/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.typeProviders.generatedCode.internal.visitor

import java.lang.StringBuilder
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.typeProviders.generatedCode.GeneratedCode
import kotlin.script.experimental.typeProviders.generatedCode.impl.appendJoined
import kotlin.script.experimental.typeProviders.generatedCode.internal.visit

internal interface GeneratedCodeVisitor {
    fun writeScript(build: StringBuilder.() -> Unit) {}

    fun withSerialized(value: Any, block: (String) -> Unit) {}

    fun useImport(import: String) {}

    fun includeScript(code: SourceCode) {}

    fun <T : GeneratedCode> StringBuilder.writeJoined(
        items: Iterable<T>,
        separator: String,
        indent: Int = 0
    ) = appendJoined(items, separator) { visit(it, indent) }

    companion object {
        operator fun invoke(vararg visitors: GeneratedCodeVisitor): GeneratedCodeVisitor = CompositeGeneratedCodeVisitor(visitors.asList())
    }
}

private class CompositeGeneratedCodeVisitor(val visitors: List<GeneratedCodeVisitor>) : GeneratedCodeVisitor {
    override fun writeScript(build: StringBuilder.() -> Unit) {
        visitors.forEach { it.writeScript(build) }
    }

    override fun withSerialized(value: Any, block: (String) -> Unit) {
        visitors.forEach { it.withSerialized(value, block) }
    }

    override fun useImport(import: String) {
        visitors.forEach { it.useImport(import) }
    }

    override fun includeScript(code: SourceCode) {
        visitors.forEach { it.includeScript(code) }
    }
}