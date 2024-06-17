/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Named
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.util.*

enum class NativeBuildType(
    val optimized: Boolean,
    val debuggable: Boolean
) : Named {
    RELEASE(true, false),
    DEBUG(false, true);

    override fun getName(): String = name.toLowerCase(Locale.ENGLISH)

    @OptIn(InternalKotlinGradlePluginApi::class)
    @Suppress("UNUSED_PARAMETER")
    @Deprecated(BITCODE_EMBEDDING_DEPRECATION_MESSAGE, ReplaceWith(""))
    fun embedBitcode(target: KonanTarget) = BitcodeEmbeddingMode.DISABLE

    companion object {
        val DEFAULT_BUILD_TYPES = setOf(DEBUG, RELEASE)
    }
}

enum class NativeOutputKind(
    val compilerOutputKind: CompilerOutputKind,
    val taskNameClassifier: String,
    val description: String = taskNameClassifier
) {
    EXECUTABLE(
        CompilerOutputKind.PROGRAM,
        "executable",
        description = "an executable"
    ),
    TEST(
        CompilerOutputKind.PROGRAM,
        "test",
        description = "a test executable"
    ),
    DYNAMIC(
        CompilerOutputKind.DYNAMIC,
        "shared",
        description = "a dynamic library"
    ),
    STATIC(
        CompilerOutputKind.STATIC,
        "static",
        description = "a static library"
    ),
    FRAMEWORK(
        CompilerOutputKind.FRAMEWORK,
        "framework",
        description = "a framework"
    ) {
        override fun availableFor(target: KonanTarget) =
            target.family.isAppleFamily
    };

    open fun availableFor(target: KonanTarget) = true
}

enum class BitcodeEmbeddingMode {
    /** Don't embed LLVM IR bitcode. */
    DISABLE,

    /** Embed LLVM IR bitcode as data. */
    BITCODE,

    /** Embed placeholder LLVM IR data as a marker. */
    MARKER,
}

@InternalKotlinGradlePluginApi
const val BITCODE_EMBEDDING_DEPRECATION_MESSAGE = "Bitcode embedding is not supported anymore. Configuring it has no effect. Corresponding DSL parameters will be removed in Kotlin 2.2"
