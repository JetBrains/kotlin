/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.naming

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

    if (!className.contains('$')) return Pair(packageName, className)

    val segments = mutableListOf<String>()
    val builder = StringBuilder()

    for (idx in className.indices) {
        val c = className[idx]
        // Don't treat a character as a separator if:
        // - it's not a '$'
        // - it's at the beginning of the segment
        // - it's the last character of the string
        if (c != '$' || builder.isEmpty() || idx == className.length - 1) {
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

    val correctedClassName = segments.joinToString(separator = ".")
    return Pair(packageName, correctedClassName)
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
