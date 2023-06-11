/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode.*
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.utils.XcodeUtils.bitcodeEmbeddingMode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.FRAMEWORK
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.target.XcodeVersion
import org.junit.Test
import kotlin.test.assertEquals

class XcodeUtilsTest {

    @Test
    fun `test bitcode embedding mode selection`() {
        val project = buildProject()

        fun xcode(major: Int, minor: Int): Provider<RegularFile> {
            val version = XcodeVersion(major, minor).toString()
            val file = project.file(version)
            file.writeText(version)
            return project.layout.file(project.provider { file })
        }

        assertEquals(DISABLE, bitcodeEmbeddingMode(FRAMEWORK, null, xcode(14, 1), IOS_ARM64, debuggable = true))
        assertEquals(MARKER, bitcodeEmbeddingMode(FRAMEWORK, null, xcode(13, 9), IOS_ARM64, debuggable = true))
        assertEquals(BITCODE, bitcodeEmbeddingMode(FRAMEWORK, null, xcode(13, 4), IOS_ARM64, debuggable = false))
        assertEquals(DISABLE, bitcodeEmbeddingMode(FRAMEWORK, null, xcode(10, 3), IOS_X64, debuggable = true))
        assertEquals(DISABLE, bitcodeEmbeddingMode(FRAMEWORK, null, xcode(13, 4), MACOS_ARM64, debuggable = false))
    }
}