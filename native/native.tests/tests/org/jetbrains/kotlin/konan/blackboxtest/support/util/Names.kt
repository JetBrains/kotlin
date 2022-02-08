/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

internal val Class<*>.sanitizedName: String get() = sanitize(name)

internal fun getSanitizedFileName(fileName: String): String = sanitize(fileName, allowDots = true)

private fun sanitize(s: String, allowDots: Boolean = false) = buildString {
    s.forEach { ch ->
        append(
            when {
                ch.isLetterOrDigit() || ch == '_' -> ch
                allowDots && ch == '.' -> ch
                else -> '_'
            }
        )
    }
}

internal const val DEFAULT_FILE_NAME = "main.kt"
internal const val LAUNCHER_FILE_NAME = "__launcher__.kt"

internal const val DEFAULT_MODULE_NAME = "default"
internal const val SUPPORT_MODULE_NAME = "support"
internal const val LAUNCHER_MODULE_NAME = "__launcher__" // Used only in KLIB tests.

internal const val STATIC_CACHE_DIR_NAME = "__static_cache__"

internal fun prettyHash(hash: Int): String = hash.toUInt().toString(16).padStart(8, '0')
