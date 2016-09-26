package org.jetbrains.kotlin.gradle.util

fun String.addBeforeSubstring(prefix: String, substring: String): String =
        replace(substring, prefix + substring)