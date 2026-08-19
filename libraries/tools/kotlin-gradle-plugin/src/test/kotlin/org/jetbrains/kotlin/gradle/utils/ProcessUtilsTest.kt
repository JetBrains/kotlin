/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.condition.OS
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProcessUtilsTest {

    private val isWindows = OS.WINDOWS.isCurrentOs

    @Test
    fun `stdout and stderr are captured correctly`() {
        var capturedResult: RunProcessResult? = null
        val command = if (isWindows) {
            listOf("cmd", "/c", "echo hello stdout & echo hello stderr 1>&2 & exit 1")
        } else {
            listOf("sh", "-c", "printf 'hello stdout'; printf 'hello stderr' >&2; exit 1")
        }
        runCommandWithFallback(
            command = command,
            fallback = { result ->
                capturedResult = result
                CommandFallback.Action("fallback")
            }
        )
        assertNotNull(capturedResult)
        assertEquals("hello stdout", capturedResult.stdOut.trim())
        assertEquals("hello stderr", capturedResult.stdErr.trim())
    }

    @Test
    @Timeout(2L, unit = TimeUnit.SECONDS)
    fun `when stdin is requested process returns normally without hanging`() {
        // sh writes to stdout and stderr, then cat reads stdin; closed stdin delivers EOF so cat exits immediately
        val command = if (isWindows) {
            listOf("cmd", "/c", "echo out & echo err 1>&2 & findstr .*")
        } else {
            listOf("sh", "-c", "printf 'out'; printf 'err' >&2; cat")
        }
        val output = runCommandWithFallback(
            command = command,
            fallback = { result ->
                CommandFallback.Error("unexpected failure: retCode=${result.retCode} stderr=${result.stdErr}")
            }
        )
        assertEquals("out", output.trim())
    }
}
