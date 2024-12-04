/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.utils

object AccessorNames {
    const val IS = "is"
    const val GET = "get"
    const val SET = "set"
}

fun toPropertyName(name: String, prefixesToStrip: List<String> = emptyList()): String =
    name.stripPrefixes(prefixesToStrip).decapitalize()

fun toPropertyNameCapitalized(name: String, prefixesToStrip: List<String> = emptyList()): String =
    name.stripPrefixes(prefixesToStrip).capitalize()

private fun String.stripPrefixes(prefixes: List<String>): String =
    prefixes.firstOrNull { isPrefix(it) }?.let { prefix ->
        drop(prefix.length)
    } ?: this


private fun String.isPrefix(prefix: String): Boolean =
    length > prefix.length && startsWith(prefix) && get(prefix.length).isUpperCase()
