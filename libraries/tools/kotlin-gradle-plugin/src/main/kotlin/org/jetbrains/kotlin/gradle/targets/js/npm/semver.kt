/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

internal fun fixSemver(version: String): String {
    var i = 0
    var number = 0
    var major = "0"
    var minor = "0"
    var patch = "0"
    val rest = StringBuilder()
    val digits = StringBuilder()

    fun setComponent() {
        val digitsFiltered = digits.toString().trimStart { it == '0' }.takeIf { it.isNotEmpty() } ?: "0"
        when (number) {
            0 -> major = digitsFiltered
            1 -> minor = digitsFiltered
            2 -> patch = digitsFiltered
            else -> error(number)
        }
    }

    while (i < version.length) {
        val c = version[i++]
        if (c.isDigit()) digits.append(c)
        else if (c == '-' || c == '+') {
            // examples:
            // 1.2.3-RC1-1234,
            // 1.2-RC1-1234
            // 1.2-beta.11+sha.0x
            setComponent()
            number = 3
            rest.append(c)
            break
        } else if (c == '.') {
            rest.append(c)
            setComponent()
            digits.setLength(0)
            number++
            if (number > 2) break
        } else rest.append(c)
    }

    if (number <= 2) setComponent()

    rest.append(version.substring(i))

    val restFiltered = rest.filter {
        it in '0'..'9' ||
                it in 'A'..'Z' ||
                it in 'a'..'z' ||
                it == '.' ||
                it == '-' ||
                it == '+'
    }
    val restComponents = restFiltered.split('+', limit = 2)

    val preRelease = restComponents.getOrNull(0)
        ?.foldDelimiters()
        ?.trim { it == '-' || it == '.' }
        ?.takeIf { it.isNotEmpty() }

    val build = restComponents.getOrNull(1)
        ?.filter { it != '+' }
        ?.trim { it == '-' || it == '.' }
        ?.takeIf { it.isNotEmpty() }

    return "$major.$minor.$patch" +
            (if (preRelease != null) "-$preRelease" else "") +
            (if (build != null) "+$build" else "")
}

private fun String.foldDelimiters(): String {
    val result = StringBuilder(length)
    var endsWithDelimiter = false
    for (i in 0 until length) {
        val c = this[i]
        if (c == '+' || c == '-' || c == '.') {
            if (!endsWithDelimiter) {
                result.append(c)
                endsWithDelimiter = true
            }
        } else {
            endsWithDelimiter = false
            result.append(c)
        }
    }
    return result.toString()
}