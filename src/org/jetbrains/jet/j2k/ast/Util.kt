package org.jetbrains.jet.j2k.ast

import java.util.List

fun List<out Node>.toKotlin(separator: String): String {
    val result = StringBuilder()
    for(x in this) {
        if (result.length() > 0) result.append(separator)
        result.append(x.toKotlin())
    }
    return result.toString()!!
}
