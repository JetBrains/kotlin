/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.*
import org.jetbrains.kotlin.gradle.utils.XcodeUtils.bitcodeEmbeddingMode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.FRAMEWORK
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.target.XcodeVersion
import org.junit.Test
import kotlin.test.assertEquals

class XcodeUtilsTest {

    @Test
    fun `test bitcode embedding mode selection`() {
        assertEquals(DISABLE, bitcodeEmbeddingMode(FRAMEWORK, null, XcodeVersion(14, 1), IOS_ARM64, debuggable = true))
        assertEquals(MARKER, bitcodeEmbeddingMode(FRAMEWORK, null, XcodeVersion(13, 9), IOS_ARM64, debuggable = true))
        assertEquals(BITCODE, bitcodeEmbeddingMode(FRAMEWORK, null, XcodeVersion(13, 4), IOS_ARM64, debuggable = false))
        assertEquals(DISABLE, bitcodeEmbeddingMode(FRAMEWORK, null, XcodeVersion(10, 3), IOS_X64, debuggable = true))
        assertEquals(DISABLE, bitcodeEmbeddingMode(FRAMEWORK, null, XcodeVersion(13, 4), MACOS_ARM64, debuggable = false))
    }
}