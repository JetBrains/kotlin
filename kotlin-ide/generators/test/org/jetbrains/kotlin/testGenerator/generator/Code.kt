/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testGenerator.generator

import org.jetbrains.kotlin.testGenerator.model.TAnnotation
import javax.lang.model.element.Modifier

interface RenderElement {
    fun Code.render()
}

class Code(private val appendable: Appendable, private val indent: String = "    ") {
    private var depth: Int = 0
    private var lastNewLine = false

    fun append(str: CharSequence): Code {
        var isFirst = true
        for (line in str.lineSequence()) {
            if (isFirst) {
                isFirst = false
            } else {
                newLine()
            }

            appendable.append(line)
        }
        lastNewLine = str.lastOrNull() == '\n'
        return this
    }

    fun newLine(): Code {
        appendable.append(System.lineSeparator())
        for (i in 1..depth) {
            appendable.append(indent)
        }
        lastNewLine = true
        return this
    }

    fun newLineIfNeeded(): Code {
        if (!lastNewLine) {
            newLine()
        }
        return this
    }

    fun incDepth(): Code {
        depth += 1
        return this
    }

    fun decDepth(): Code {
        depth -= 1
        return this
    }
}

fun buildCode(block: Code.() -> Unit): String {
    val builder = StringBuilder()
    Code(builder).apply(block)
    return builder.toString()
}

fun Code.appendLine(text: String) {
    append(text)
    newLine()
}

fun Code.appendBlock(text: String = "", block: Code.() -> Unit) {
    if (text.isNotEmpty()) {
        append(text)
        append(" ")
    }

    append("{")
    incDepth()
    newLine()
    block()
    decDepth()
    newLine()
    append("}")
}

fun Code.append(element: RenderElement): Code {
    with(element) { render() }
    return this
}

fun <T : RenderElement> Code.appendList(
    list: Collection<T>,
    separator: String = ", ",
    prefix: String = "",
    postfix: String = ""
): Code {
    return appendList(list, separator, prefix, postfix) { append(it) }
}

fun <T> Code.appendList(
    list: Collection<T>,
    separator: String = ", ",
    prefix: String = "",
    postfix: String = "",
    block: Code.(T) -> Unit
): Code {
    if (list.isEmpty()) {
        return this
    }

    append(prefix)
    var isFirst = true
    for (item in list) {
        if (isFirst) {
            isFirst = false
        } else {
            append(separator)
        }

        block(item)
    }
    append(postfix)
    return this
}

fun Code.appendModifiers(modifiers: Set<Modifier>): Code {
    appendList(modifiers, " ") { append(it.toString()) }
    if (modifiers.isNotEmpty()) {
        append(" ")
    }
    return this
}

fun Code.appendMultilineComment(text: String?): Code {
    if (text != null && text.isNotBlank()) {
        append("/*").newLine()
        for (line in text.lineSequence()) {
            append(" * ").append(line).newLine()
        }
        append(" */").newLine()
    }

    return this
}

fun Code.appendAnnotation(annotation: TAnnotation) {
    append("@${annotation.simpleName}")
    if (annotation.args.isNotEmpty()) {
        val args = annotation.args.joinToString(prefix = "(", postfix = ")") { it.render() }
        append(args)
    }
    newLine()
}