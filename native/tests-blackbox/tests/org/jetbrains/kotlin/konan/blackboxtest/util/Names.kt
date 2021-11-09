/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.util

internal val Class<*>.sanitizedName: String
    get() = name.map { if (it.isLetterOrDigit() || it == '_') it else '_' }.joinToString("")

internal fun getSanitizedFileName(fileName: String): String =
    fileName.map { if (it.isLetterOrDigit() || it == '_' || it == '.') it else '_' }.joinToString("")

internal const val DEFAULT_FILE_NAME = "main.kt"

internal const val DEFAULT_MODULE_NAME = "default"
internal const val SUPPORT_MODULE_NAME = "support"

internal fun prettyHash(hash: Int): String = hash.toUInt().toString(16).padStart(8, '0')
