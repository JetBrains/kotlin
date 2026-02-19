/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.impl.naming

/**
 * Convert JVM internal binary name to Java canonical name.
 * Examples:
 *  - `foo/bar/Biz$Gz` -> `foo.bar.Biz.Gz`
 *  - `foo/bar/Biz$$$Gz$$X` -> `foo.bar.Biz.$$Gz.$X`
 *  - `foo/bar/Biz` -> `foo.bar.Biz`
 *  - `Foo$Bar` -> `Foo.Bar`
 *  - `Foo` -> `Foo`
 */
internal fun String.jvmInternalToCanonical(): Pair<String, String> {
    val lastDelimiter = lastIndexOf('/')

    val packageName = if (lastDelimiter >= 0) {
        substring(0, lastDelimiter).replace('/', '.')
    } else {
        ""
    }

    val className = substring(lastDelimiter + 1)
    return Pair(packageName, className.replaceDollars())
}

/**
 * Convert JVM descriptor of object type of parameter or annotation to Java canonical name.
 * Examples:
 *  - `Lfoo/bar/Biz$Gz;` -> `foo.bar.Biz.Gz`
 *  - `Lfoo/bar/Biz$$$Gz$$X;` -> `foo.bar.Biz.$$Gz.$X`
 *  - `LBar;` -> `Bar`
 */
internal fun String.jvmTypeDescToCanonical(): Pair<String, String> {
    return substring(1, length - 1).jvmInternalToCanonical()
}

/**
 * Replaces `$` separator characters in the string with a dot-separated format.
 * If the `$` character is repeated in a row, then only the first one is replaced with dot.
 *
 * Examples:
 *  - `Biz$Gz` -> `Biz.Gz`
 *  - `Biz$$$Gz$$X` -> `Biz.$$Gz.$X`
 *  - `Bar` -> `Bar`
 */
private fun String.replaceDollars(): String {
    if (!contains('$')) return this

    val segments = mutableListOf<String>()
    val builder = StringBuilder()
    for (idx in indices) {
        val c = this[idx]
        // Don't treat a character as a separator if:
        // - it's not a '$'
        // - it's at the beginning of the segment
        // - it's the last character of the string
        if (c != '$' || builder.isEmpty() || idx == length - 1) {
            builder.append(c)
            continue
        }
        check(c == '$')
        // class$$$subclass -> class.$$subclass, were at second $ here.
        if (builder.last() == '$') {
            builder.append(c)
            continue
        }

        segments.add(builder.toString())
        builder.clear()
    }
    if (builder.isNotEmpty()) {
        segments.add(builder.toString())
    }

    return segments.joinToString(separator = ".")
}
