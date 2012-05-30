package org.jetbrains.jet.j2k.ast

import java.util.List

fun List<out Node>.toKotlin(separator: String, prefix: String = "", suffix: String = ""): String {
    val result = StringBuilder()
    if (size() > 0) {
        result.append(prefix)
        var first = true
        for(x in this) {
            if (!first) result.append(separator)
            first = false
            result.append(x.toKotlin())
        }
        result.append(suffix)
    }
    return result.toString()!!
}

fun String.withPrefix(prefix: String) = if (isEmpty()) "" else prefix + this
fun Expression.withPrefix(prefix: String) = if (isEmpty()) "" else prefix + toKotlin()
