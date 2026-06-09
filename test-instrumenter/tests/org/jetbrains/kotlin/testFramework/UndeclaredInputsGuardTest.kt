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

    private lateinit var undeclaredInputsGuard: UndeclaredInputsGuard

    @BeforeEach
    fun setup() {
        val rootDir = File("..").canonicalPath
        val buildDir = File("build").canonicalPath
        val declaredInputs = emptySet<String>()

        UndeclaredInputsGuard.install(rootDir, buildDir, declaredInputs)
        undeclaredInputsGuard = UndeclaredInputsGuard.getInstance()
    }

    @Test
    fun `guard detects undeclared inout`() {
        // when
        undeclaredInputsGuard.checkPath("foo.txt")

        // then
        assertTrue { undeclaredInputsGuard.undeclaredInputs.isNotEmpty() }
    }

    @Test
    fun `guard is thread-safe`() {
        // when
        (1..1000)
            .map {
                thread {
                    Thread.sleep(Random.nextLong(0, 500))
                    undeclaredInputsGuard.checkPath("$it.txt")
                }
            }
            .forEach { it.join() }

        // then
        assertEquals(1000, undeclaredInputsGuard.undeclaredInputs.size)
    }
}
