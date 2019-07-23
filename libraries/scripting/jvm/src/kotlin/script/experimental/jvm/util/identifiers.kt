/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.util

fun String.toValidJvmIdentifier(): String =
    if (isBlank()) "_".repeat(length)
    else buildString(length) {
        for (ch in this@toValidJvmIdentifier) {
            // encoding is taken from https://blogs.oracle.com/jrose/symbolic-freedom-in-the-vm
            when (ch) {
                '/' -> append("\\|")
                '.' -> append("\\,")
                ';' -> append("\\?")
                '$' -> append("\\%")
                '<' -> append("\\^")
                '>' -> append("\\_")
                '[' -> append("\\{")
                ']' -> append("\\}")
                ':' -> append("\\!")
                '\\' -> append("\\-")
                else -> append(ch)
            }
        }
    }