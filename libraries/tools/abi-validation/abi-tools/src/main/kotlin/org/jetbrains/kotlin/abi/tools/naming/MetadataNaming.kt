/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.naming

/**
 * Convert names from kotlin metadata to Kotlin qualified name.
 * Examples:
 * - `foo/bar/Biz` -> `foo.bar.Biz`
 * - `foo/bar/Biz.Gz` -> `foo.bar.Biz.Gz`
 * - `foo/bar/$Biz.$Gz` -> `foo.bar.$Biz.$Gz`
 * - `Foo` -> `Foo`
 */
internal fun String.metadataNameToQualified(): Pair<String, String> {
    val lastDelimiter = lastIndexOf('/')

    val packageName = if (lastDelimiter >= 0) {
        substring(0, lastDelimiter).replace('/', '.')
    } else {
        ""
    }
    val className = substring(lastDelimiter + 1)

    return packageName to className
}