package org.jetbrains.kotlin.gradle.util

import java.util.*

fun String.addBeforeSubstring(prefix: String, substring: String): String =
    replace(substring, prefix + substring)

fun String.checkedReplace(original: String, replacement: String): String {
    check(contains(original)) { "Substring '$original' is not found in '$this'" }
    return replace(original, replacement)
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
fun String.removingTrailingNewline(): String = this.dropLastWhile { it == '\n' }