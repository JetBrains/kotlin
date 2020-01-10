/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.stubs.util

import org.jetbrains.kaptlite.signature.SigType
import org.jetbrains.kaptlite.signature.SigTypeArgument
import org.jetbrains.kaptlite.signature.SigTypeParameter
import javax.lang.model.element.Modifier

interface Renderable {
    fun CodeScope.render()
}

class CodeScope(private val appendable: Appendable, val indent: String = "    ") {
    private var depth: Int = 0
    private var lastNewLine = false

    fun append(c: Char): CodeScope {
        if (c == '\n') {
            newLine()
        } else {
            appendable.append(c)
        }
        lastNewLine = c == '\n'
        return this
    }

    fun append(str: CharSequence): CodeScope {
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

    fun newLine(): CodeScope {
        appendable.append(System.lineSeparator())
        for (i in 1..depth) {
            appendable.append(indent)
        }
        lastNewLine = true
        return this
    }

    fun newLineIfNeeded(): CodeScope {
        if (!lastNewLine) {
            newLine()
        }
        return this
    }

    fun incDepth(): CodeScope {
        depth += 1
        return this
    }

    fun decDepth(): CodeScope {
        depth -= 1
        return this
    }
}

fun CodeScope.block(block: CodeScope.() -> Unit) {
    append("{")
    incDepth()
    newLine()
    block()
    decDepth()
    newLine()
    append("}")
}

fun CodeScope.append(renderable: Renderable): CodeScope {
    with(renderable) { render() }
    return this
}

fun CodeScope.append(type: SigType): CodeScope {
    when (type) {
        is SigType.TypeVariable -> append(type.name)
        is SigType.Array -> append(type.elementType).append("[]")
        is SigType.Primitive -> append(type.javaName)
        is SigType.Class -> append(type.fqName)
        is SigType.Nested -> append(type.outer).append('.').append(type.name)
        is SigType.Generic -> {
            append(type.base)
            appendList(type.args, prefix = "<", postfix = ">") { append(it) }
        }
    }
    return this
}

fun CodeScope.append(param: SigTypeParameter): CodeScope {
    append(param.name)
    appendList(param.bounds, " & ", prefix = " extends ") { append(it) }
    return this
}

fun CodeScope.append(arg: SigTypeArgument): CodeScope {
    when (arg) {
        SigTypeArgument.Unbound -> append('?')
        is SigTypeArgument.Invariant -> append(arg.type)
        is SigTypeArgument.Extends -> append("? extends ").append(arg.type)
        is SigTypeArgument.Super -> append("? super ").append(arg.type)
    }
    return this
}

fun <T : Renderable> CodeScope.appendList(
    list: Collection<T>,
    separator: String = ", ",
    prefix: String = "",
    postfix: String = ""
): CodeScope {
    return appendList(list, separator, prefix, postfix) { append(it) }
}

fun <T> CodeScope.appendList(
    list: Collection<T>,
    separator: String = ", ",
    prefix: String = "",
    postfix: String = "",
    block: CodeScope.(T) -> Unit
): CodeScope {
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

fun CodeScope.appendModifiers(modifiers: Set<Modifier>): CodeScope {
    appendList(modifiers, " ") { append(it.toString()) }
    return this
}

fun CodeScope.appendJavadoc(text: String?): CodeScope {
    if (text != null && text.isNotBlank()) {
        append("/**").newLine()
        for (line in text.lineSequence()) {
            append(" * ").append(line).newLine()
        }
        append(" */").newLine()
    }

    return this
}