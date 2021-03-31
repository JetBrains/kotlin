/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.utils

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.lombok.config.Accessors

object AccessorNames {
    const val IS = "is"
    const val GET = "get"
    const val SET = "set"
}

/**
 * Make property name from variable name
 * Returns null in case getter/setter shouldn't be generated at all
 */
fun PropertyDescriptor.toAccessorBaseName(config: Accessors): String? {
    val isPrimitiveBoolean = type.isPrimitiveBoolean()
    return if (config.prefix.isEmpty()) {
        val prefixes = if (isPrimitiveBoolean) listOf(AccessorNames.IS) else emptyList()
        toPropertyName(name.identifier, prefixes)
    } else {
        val id = name.identifier
        val name = toPropertyName(id, config.prefix)
        name.takeIf { it.length != id.length}
    }
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
