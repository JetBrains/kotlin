/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.filtering

/**
 * Replaces characters `**` to `.*`, `*` to `[^.]*` and `?` to `.` regexp characters and also add escape char '\' before regexp metacharacters (see [regexMetacharactersSet]).
 */
internal fun String.wildcardsToRegex(): Regex {
    // in most cases, the characters `*` or `.` will be present therefore, we increase the capacity in advance
    val builder = StringBuilder(length * 2)
    var prevCharIsStar = false

    fun checkStar() {
        if (prevCharIsStar) {
            builder.append("[^.]*")
        }
        prevCharIsStar = false
    }

    forEach { char ->
        when (char) {
            in regexMetacharactersSet -> {
                checkStar()
                builder.append('\\').append(char)
            }
            '*' -> {
                if (prevCharIsStar) {
                    builder.append(".*")
                    prevCharIsStar = false
                } else {
                    prevCharIsStar = true
                }
            }
            '?' -> {
                checkStar()
                builder.append('.')
            }
            else -> {
                checkStar()
                builder.append(char)
            }
        }
    }
    if (prevCharIsStar) {
        builder.append("[^.]*")
    }

    return builder.toString().toRegex()
}

private val regexMetacharactersSet = "<([{\\^-=$!|]})+.>".toSet()