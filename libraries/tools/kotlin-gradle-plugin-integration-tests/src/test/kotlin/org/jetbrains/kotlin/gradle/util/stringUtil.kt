package org.jetbrains.kotlin.gradle.util

fun String.addBeforeSubstring(prefix: String, substring: String): String =
    replace(substring, prefix + substring)

fun String.checkedReplace(original: String, replacement: String): String {
    check(contains(original)) { "Substring '$original' is not found in '$this'" }
    return replace(original, replacement)
}