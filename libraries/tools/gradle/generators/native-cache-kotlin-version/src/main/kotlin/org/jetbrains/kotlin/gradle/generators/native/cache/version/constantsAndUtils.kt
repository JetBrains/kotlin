/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.generators.native.cache.version

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil

internal const val KGP_PACKAGE = "org.jetbrains.kotlin.gradle.plugin"
internal const val KGP_MPP_PACKAGE = "$KGP_PACKAGE.mpp"
internal const val CLASS_KDOC = """
Provides type-safe constants for Kotlin versions to be used in the DSL for disabling the native cache.

Disabling the native cache is not recommended and should only be used as a temporary workaround.
This class follows a rolling deprecation cycle to ensure that any cache-disabling configuration
is reviewed after a Kotlin update.

Only the 3 most recent versions are included:
- **N (Latest):** The version constant is available.
- **N-1 (Deprecated):** The constant is marked with a deprecation warning.
- **N-2 (Error):** The constant is marked with a deprecation error.
- **N-3 (Dropped):** The constant is removed, causing a compilation failure.

This forces a review of the cache-disabling configuration. If the problem is resolved,
please remove the DSL entry. If not, please update to the latest version constant.

@since 2.3.20
"""
internal val ANNOTATION_NATIVE_CACHE_API = ClassName(KGP_MPP_PACKAGE, "KotlinNativeCacheApi")

internal fun createGeneratedFileAppendable(): StringBuilder = StringBuilder(GeneratorsFileUtil.GENERATED_MESSAGE_PREFIX)
    .appendLine("the README.md file").appendLine(GeneratorsFileUtil.GENERATED_MESSAGE_SUFFIX).appendLine()

internal fun parseKotlinVersion(kotlinVersionString: String): Triple<Int, Int, Int> {
    // Handle suffixes, "v" prefix, and underscores all at once
    val baseVersion = kotlinVersionString.split("-", limit = 2)[0]
        .removePrefix("v") // Handle "v2_3_0"
        .replace('_', '.')   // Handle "2_3_0" -> "2.3.0"

    val baseVersionSplit = baseVersion.split(".")

    val majorVersion =
        baseVersionSplit[0].toIntOrNull() ?: error("Invalid Kotlin version: $kotlinVersionString (Failed parsing major version)")
    val minorVersion =
        baseVersionSplit.getOrNull(1)?.toIntOrNull() ?: error("Invalid Kotlin version: $kotlinVersionString (Failed parsing minor version)")
    val patchVersion = baseVersionSplit.getOrNull(2)?.toIntOrNull() ?: 0

    return Triple(majorVersion, minorVersion, patchVersion)
}