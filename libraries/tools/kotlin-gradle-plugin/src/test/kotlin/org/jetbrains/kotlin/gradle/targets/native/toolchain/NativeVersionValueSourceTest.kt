/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeVersionValueSourceTest {

    @Rule
    @JvmField
    var tmp = TemporaryFolder()

    @Test
    fun testMoveToNonEmptyDir() {
        val fromDir = tmp.newFolder()
        fromDir.resolve("A.kt").also {
            it.createNewFile()
            it.writeText("class A {}")
        }
        fromDir.resolve("B.kt").createNewFile()

        val toDir = tmp.newFolder()
        toDir.resolve("A.kt").createNewFile()
        toDir.resolve("C.kt").createNewFile()

        NativeVersionValueSource.Companion.copyNativeBundleDistribution(fromDir, toDir)
        assertEquals("class A {}", toDir.resolve("A.kt").readText())
        assertTrue("File B.kt should be copied from directory") { toDir.resolve("B.kt").exists() }
        assertTrue("C.kt file should not be removed") { toDir.resolve("C.kt").exists() }
        assertTrue("Marker file should be created") { toDir.resolve(NativeVersionValueSource.Companion.MARKER_FILE).exists() }
    }
}