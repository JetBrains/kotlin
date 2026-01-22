/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.jetbrains.kotlin.gradle.testing.WithTemporaryFolder
import org.jetbrains.kotlin.gradle.testing.newTempDirectory
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeVersionValueSourceTest: WithTemporaryFolder {

    @field:TempDir
    override lateinit var temporaryFolder: Path

    @Test
    fun testMoveToNonEmptyDir() {
        val fromDir = newTempDirectory().toFile()
        fromDir.resolve("A.kt").also {
            it.createNewFile()
            it.writeText("class A {}")
        }
        fromDir.resolve("B.kt").createNewFile()

        val toDir = newTempDirectory().toFile()
        toDir.resolve("A.kt").createNewFile()
        toDir.resolve("C.kt").createNewFile()

        NativeVersionValueSource.copyNativeBundleDistribution(fromDir, toDir)
        assertEquals("class A {}", toDir.resolve("A.kt").readText())
        assertTrue("File B.kt should be copied from directory") { toDir.resolve("B.kt").exists() }
        assertTrue("C.kt file should not be removed") { toDir.resolve("C.kt").exists() }
        assertTrue("Marker file should be created") { toDir.resolve(NativeVersionValueSource.Companion.MARKER_FILE).exists() }
    }
}
