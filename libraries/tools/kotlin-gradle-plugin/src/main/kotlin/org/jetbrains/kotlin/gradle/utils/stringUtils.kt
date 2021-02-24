/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

internal fun lowerCamelCaseName(vararg nameParts: String?): String {
    val nonEmptyParts = nameParts.mapNotNull { it?.takeIf(String::isNotEmpty) }
    return nonEmptyParts.drop(1).joinToString(
        separator = "",
        prefix = nonEmptyParts.firstOrNull().orEmpty(),
        transform = String::capitalize
    )
}

internal fun dashSeparatedName(nameParts: Iterable<String?>) = dashSeparatedName(*nameParts.toList().toTypedArray())

internal fun dashSeparatedName(vararg nameParts: String?): String {
    val nonEmptyParts = nameParts.mapNotNull { it?.takeIf(String::isNotEmpty) }
    return nonEmptyParts.joinToString(separator = "-")
}

internal fun String.decamelize(): String {
    return replace(upperCaseRegex) {
        val (first) = it.destructured
        "-${first.toLowerCase()}"
    }
}

private val upperCaseRegex = "([A-Z])".toRegex()

private val invalidTaskNameCharacters = "[/\\\\:<>\"?*|]".toRegex()

/**
 * Replaces characters which are not allowed in Gradle task names (/, \, :, <, >, ", ?, *, |) with '_'
 */
internal fun String.asValidTaskName() = replace(invalidTaskNameCharacters, "_")

private val ANSI_COLOR_REGEX = "\\x1b\\[[0-9;]*m".toRegex()

internal fun String.clearAnsiColor() =
    replace(ANSI_COLOR_REGEX, "")