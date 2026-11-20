/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.utilities

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.jupiter.api.Test
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.test.assertEquals

class CacheCompilerArgsTest {
    private val library = Path("klib/common/stdlib")
    private val cacheInfo = CacheInfo(Path("cache"), "static_cache", listOf("-g"))

    // KT-90608
    @Test
    fun konanDataDirIsPassed() {
        assertEquals(
                listOf(
                        "-p", "static_cache",
                        "-target", "linux_x64",
                        "-Xadd-cache=${library.absolutePathString()}",
                        "-Xcache-directory=${Path("cache").absolutePathString()}",
                        "-Xkonan-data-dir=/tmp/konan-data",
                        "-g",
                ),
                cacheCompilerArgs(KonanTarget.LINUX_X64, library, cacheInfo, "/tmp/konan-data")
        )
    }

    @Test
    fun konanDataDirIsOmittedWhenNotSet() {
        assertEquals(
                listOf(
                        "-p", "static_cache",
                        "-target", "linux_x64",
                        "-Xadd-cache=${library.absolutePathString()}",
                        "-Xcache-directory=${Path("cache").absolutePathString()}",
                        "-g",
                ),
                cacheCompilerArgs(KonanTarget.LINUX_X64, library, cacheInfo, konanDataDir = null)
        )
    }
}
