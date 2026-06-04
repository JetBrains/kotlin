/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework

import org.jetbrains.kotlin.testFramework.inputchecking.UndeclaredInputsGuard
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText
import kotlin.random.Random

class UndeclaredInputsGuardTest {

    @TempDir
    private lateinit var tempDir: Path

    private lateinit var declaredInputsFile: Path

    @BeforeEach
    fun setup() {
        declaredInputsFile = tempDir.resolve("declared-inputs.txt").apply { writeText("") }
        System.setProperty("test.instrumenter.declared.inputs.file", declaredInputsFile.absolutePathString())
        System.setProperty("test.instrumenter.root.dir", File("..").canonicalPath)
        System.setProperty("test.instrumenter.build.dir", File("build").canonicalPath)
    }

    @Test
    fun `guard detects undeclared inout`() {
        // when
        UndeclaredInputsGuard.checkPath("foo.txt")

        // then
        assertTrue { UndeclaredInputsGuard.getUndeclaredInputs().isNotEmpty() }
    }

    @Test
    fun `guard is thread-safe`() {
        // when
        (1..1000)
            .map {
                thread {
                    Thread.sleep(Random.nextLong(0, 500))
                    UndeclaredInputsGuard.checkPath("$it.txt")
                }
            }
            .forEach { it.join() }

        // then
        assertEquals(1000, UndeclaredInputsGuard.getUndeclaredInputs().size)
    }
}
