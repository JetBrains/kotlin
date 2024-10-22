/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.plugins.kotlin

import org.jetbrains.kotlin.config.LanguageVersion

/**
 * Compose metadata encoded with declarations that require special handling for backwards compatibility.
 * Current version of compiler expects the following metadata format:
 * ```
 * ┌───────────────┬───────────────┐
 * │      0        │       1       │
 * ├───────────────┼───────────────┤
 * │ major version │ minor version │
 * └───────────────┴───────────────┘
 * ```
 */
@JvmInline
value class ComposeMetadata(val data: ByteArray) {
    constructor(version: LanguageVersion) : this(byteArrayOf(version.major.toByte(), version.minor.toByte()))

    /**
     * Open functions with default params are supported 2.1.20 onwards.
     */
    fun supportsOpenFunctionsWithDefaultParams(): Boolean =
        data[0] >= 2 && data[1] >= 1
}