/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.target.Architecture.ARM32
import org.jetbrains.kotlin.konan.target.Architecture.ARM64
import org.jetbrains.kotlin.konan.target.Family.*

@InternalKotlinGradlePluginApi
object XcodeUtils {

    fun bitcodeEmbeddingMode(
        outputKind: CompilerOutputKind,
        userMode: BitcodeEmbeddingMode?,
        xcodeVersion: XcodeVersion?,
        target: KonanTarget,
        debuggable: Boolean,
    ): BitcodeEmbeddingMode {
        return when {
            outputKind != CompilerOutputKind.FRAMEWORK -> BitcodeEmbeddingMode.DISABLE
            userMode != null -> userMode
            bitcodeSupported(xcodeVersion, target) -> when (debuggable) {
                true -> BitcodeEmbeddingMode.MARKER
                false -> BitcodeEmbeddingMode.BITCODE
            }
            else -> BitcodeEmbeddingMode.DISABLE
        }
    }

    private fun bitcodeSupported(xcodeVersion: XcodeVersion?, target: KonanTarget): Boolean {
        return xcodeVersion != null
                && xcodeVersion.major < 14
                && target.family in listOf(IOS, WATCHOS, TVOS)
                && target.architecture in listOf(ARM32, ARM64)
    }
}