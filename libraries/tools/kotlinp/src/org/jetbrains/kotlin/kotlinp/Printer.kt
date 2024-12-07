/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kotlinp

inline fun printString(builder: StringBuilderPrinter.() -> Unit): String {
    return StringBuilderPrinter().apply(builder).toString()
}

abstract class Printer : Appendable {
    private var indentLevel = 0
    private var commentsMode = false
    private var onEmptyLine = true

    protected abstract val wrapped: Appendable

    operator fun plusAssign(value: Any?) {
        append(value.toString())
    }

    override fun append(charSequence: CharSequence, start: Int, end: Int): Printer = append(charSequence.subSequence(start, end))

    override fun append(charSequence: CharSequence): Printer {
        for (ch in charSequence) append(ch)
        return this
    }

    override fun append(ch: Char): Printer {
        if (ch == '\n') {
            wrapped.append('\n')
            onEmptyLine = true
        } else {
            if (onEmptyLine) {
                onEmptyLine = false
                repeat(indentLevel) { wrapped.append(INDENT_UNIT) }
                if (commentsMode) wrapped.append("// ")
            }
            wrapped.append(ch)
        }
        return this
    }

    fun append(vararg values: Any?): Printer {
        for (value in values) {
            append(value.toString())
        }
        return this
    }

    fun appendLine(vararg values: Any?): Printer {
        return append(*values).append('\n')
    }

    inline fun <T> Printer.appendCollection(
        items: Collection<T>,
        separator: String = ", ",
        prefix: String = "",
        postfix: String = "",
        appendElement: Printer.(T) -> Unit,
    ) {
        append(prefix)
        items.forEachIndexed { index, item ->
            if (index > 0) append(separator)
            appendElement(item)
        }
        append(postfix)
    }

    inline fun <T> Printer.appendCollectionIfNotEmpty(
        items: Collection<T>,
        separator: String = ", ",
        prefix: String = "",
        postfix: String = "",
        appendElement: Printer.(T) -> Unit,
    ) {
        if (items.isEmpty()) return
        appendCollection(items, separator, prefix, postfix, appendElement)
    }

    fun appendFlags(vararg modifiers: Pair<Boolean, String>) {
        for ((condition, token) in modifiers) {
            if (condition) {
                append(token, " ")
            }
        }
    }

    fun appendCommentedLine(vararg values: Any?): Printer {
        commented { appendLine(*values) }
        return this
    }

    fun commented(block: Printer.() -> Unit) {
        val hadCommentsMode = commentsMode
        try {
            commentsMode = true
            this.block()
        } finally {
            commentsMode = hadCommentsMode
        }
    }

    fun withIndent(block: Printer.() -> Unit) {
        try {
            indentLevel++
            this.block()
        } finally {
            indentLevel--
        }
    }

    companion object {
        private const val INDENT_UNIT = "  "
    }
}

class StringBuilderPrinter : Printer() {
    override val wrapped = StringBuilder()
    val isEmpty: Boolean get() = wrapped.isEmpty()
    override fun toString() = wrapped.toString()
}

fun Printer(output: Appendable): Printer = DelegatingBuilder(output)

private class DelegatingBuilder(override val wrapped: Appendable) : Printer()
