/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.util

import java.io.Serializable
import kotlin.script.experimental.api.SourceCode

data class AbsSourceCodePosition(val line: Int, val col: Int, val absolutePos: Int) : Serializable

internal fun String.findNth(s: String, n: Int, start: Int = 0): Int {
    if (n < 1) return -1

    var i = start

    for (k in 1..n) {
        i = indexOf(s, i)
        if (i == -1) return -1
        i += s.length
    }

    return i - s.length
}

fun Int.toSourceCodePosition(code: SourceCode): SourceCode.Position {
    val substr = code.text.substring(0, this)
    val line = 1 + substr.count { it == '\n' }
    val sep = code.text.determineSep()
    val col = 1 + substr.length - substr.lastIndexOf(sep) - sep.length
    return SourceCode.Position(line, col, this)
}

fun String.determineSep() = if (indexOf("\r\n") == -1) "\n" else "\r\n"

fun SourceCode.Position.calcAbsolute(code: SourceCode): Int {
    if (absolutePos != null)
        return absolutePos!!

    if (line == 1)
        return col - 1

    val sep = code.text.determineSep()
    return code.text.findNth(sep, line - 1) + sep.length + col - 1
}