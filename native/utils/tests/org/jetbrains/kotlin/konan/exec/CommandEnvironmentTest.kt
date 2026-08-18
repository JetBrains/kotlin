/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.exec

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue

class CommandEnvironmentTest {
    private val isWindows = System.getProperty("os.name").startsWith("Windows")

    @Test
    fun `environment variables are passed to the spawned process`() {
        assumeTrue(!isWindows, "POSIX shell required")
        val output = Command(listOf("/bin/sh", "-c", "printf %s \"\$KONAN_TEST_VAR\""), environment = mapOf("KONAN_TEST_VAR" to "provisioned"))
            .getOutputLines()
        assertEquals(listOf("provisioned"), output)
    }

    @Test
    fun `provided environment overrides the inherited one`() {
        assumeTrue(!isWindows, "POSIX shell required")
        // PATH is always present in the inherited environment; the explicit value must win.
        val output = Command(listOf("/bin/sh", "-c", "printf %s \"\$PATH\""), environment = mapOf("PATH" to "/xcode/override"))
            .getOutputLines()
        assertEquals(listOf("/xcode/override"), output)
    }
}
