/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.translation

import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Body
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.BodyStatement
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Definition
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.EntityId
import kotlin.math.absoluteValue

fun GlobalScopeResolver.functionObjCDeclaration(contents: LineBuilder, definition: Definition.Function) {
    contents.append("id ")
    contents.append(computeName(definition))
    contents.parens {
        arg("int32_t localsCount")
        definition.parameters.forEachIndexed { index, _ ->
            arg("id l${index}")
        }
    }
}

fun GlobalScopeResolver.initObjCDeclaration(contents: LineBuilder, definition: Definition.Class) {
    contents.append("- (instancetype)")
    contents.selector {
        selector("initWith")
        definition.fields.forEachIndexed { index, _ ->
            arg("f$index", "(id)f${index}")
        }
    }
}

class SelectorBuilder(private val lineBuilder: LineBuilder) {
    private var selector: (LineBuilder.() -> Unit)? = null
    private val args = mutableListOf<Pair<String, LineBuilder.() -> Unit>>()

    fun selector(block: LineBuilder.() -> Unit) {
        selector = block
    }

    fun selector(text: String) = selector {
        append(text)
    }

    fun arg(name: String, block: LineBuilder.() -> Unit) {
        args.add(name to block)
    }

    fun arg(name: String, text: String) = arg(name) {
        append(text)
    }

    fun build() = with(lineBuilder) {
        requireNotNull(selector)
        selector!!()
        selector = null
        args.forEachIndexed { index, (name, arg) ->
            if (index == 0) {
                append(name.replaceFirstChar { it.uppercase() })
            } else {
                append(name)
            }
            append(":")
            arg()
            if (index < args.size - 1) append(" ")
        }
        args.clear()
    }
}

private fun LineBuilder.selector(block: SelectorBuilder.() -> Unit) = SelectorBuilder(this).run {
    block()
    build()
}

class SelectorCallBuilder(private val lineBuilder: LineBuilder) {
    private var receiver: (LineBuilder.() -> Unit)? = null
    private val selectorBuilder = SelectorBuilder(lineBuilder)

    fun receiver(block: LineBuilder.() -> Unit) {
        receiver = block
    }

    fun receiver(text: String) = receiver {
        append(text)
    }

    fun selector(block: LineBuilder.() -> Unit) = selectorBuilder.selector(block)
    fun selector(text: String) = selectorBuilder.selector(text)
    fun arg(name: String, block: LineBuilder.() -> Unit) = selectorBuilder.arg(name, block)
    fun arg(name: String, text: String) = selectorBuilder.arg(name, text)

    fun build() = with(lineBuilder) {
        append("[")
        requireNotNull(receiver)
        receiver!!()
        receiver = null
        append(" ")
        selectorBuilder.build()
        append("]")
    }
}

fun LineBuilder.selectorCall(block: SelectorCallBuilder.() -> Unit) = SelectorCallBuilder(this).run {
    block()
    build()
}

class FunctionCallBuilder(private val lineBuilder: LineBuilder) {
    private var name: (LineBuilder.() -> Unit)? = null
    private val parensBuilder = ParensBuilder(lineBuilder)

    fun name(block: LineBuilder.() -> Unit) {
        name = block
    }

    fun name(text: String) = name {
        append(text)
    }

    fun arg(block: LineBuilder.() -> Unit) = parensBuilder.arg(block)
    fun arg(text: String) = parensBuilder.arg(text)

    fun build() = with(lineBuilder) {
        requireNotNull(name)
        name!!()
        name = null
        parensBuilder.build()
    }
}

fun LineBuilder.functionCall(block: FunctionCallBuilder.() -> Unit) = FunctionCallBuilder(this).run {
    block()
    build()
}

class ParensBuilder(private val lineBuilder: LineBuilder) {
    private val args = mutableListOf<LineBuilder.() -> Unit>()

    fun arg(block: LineBuilder.() -> Unit) {
        args.add(block)
    }

    fun arg(text: String) = arg {
        append(text)
    }

    fun build() = with(lineBuilder) {
        append("(")
        args.forEachIndexed { index, arg ->
            arg()
            if (index < args.size - 1) append(", ")
        }
        append(")")
        args.clear()
    }
}

fun LineBuilder.parens(block: ParensBuilder.() -> Unit) = ParensBuilder(this).run {
    block()
    build()
}

fun OutputFileBuilder.braces(block: () -> Unit) {
    lineEnd(" {")
    indent(block)
    lineEnd("}")
}

private val EntityId.normalized: EntityId
    get() = when (this) {
        Int.MIN_VALUE -> Int.MAX_VALUE
        else -> absoluteValue
    }

val EntityId.asString: String
    get() = normalized.toString()

fun <T> List<T>.findEntity(id: EntityId): T? {
    if (isEmpty()) return null
    return this[id.normalized % size]
}

fun Body.estimateLocalsCount(): Int {
    return statements.sumOf {
        when (it) {
            is BodyStatement.Call -> 1
            is BodyStatement.Store -> 0
            is BodyStatement.SpawnThread -> 0
            is BodyStatement.Alloc -> 1
            is BodyStatement.Load -> 1
        }
    }
}

fun Definition.Function.estimateLocalsCount(): Int {
    return parameters.size + body.body.estimateLocalsCount()
}