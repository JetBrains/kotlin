/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.simple

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Binaries
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.targets
import org.jetbrains.kotlin.native.executors.RunProcessException
import org.jetbrains.kotlin.native.executors.RunProcessResult
import org.jetbrains.kotlin.native.executors.runProcess
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class NativeSourceRangesTest : AbstractNativeSimpleTest() {
    private val BASE_DIR = File("native/native.tests/testData/sourceRanges/kt72356")
    private val konanHome get() = testRunSettings.get<KotlinNativeHome>().dir
    private val buildDir get() = testRunSettings.get<Binaries>().testBinariesDir
    private val konanc get() = konanHome.resolve("bin").resolve(if (HostManager.Companion.hostIsMingw) "konanc.bat" else "konanc")
    private val konancTimeout = 1.minutes

    private fun compileLibrary(vararg sourcePaths: String): RunProcessResult {
        val kexe = buildDir.resolve("kexe.kexe").also { it.delete() }
        val args = mutableListOf("-output", kexe.absolutePath).apply {
            add("-target")
            add(targets.testTarget.visibleName)
            add("-produce")
            add("library")
        }

        return runProcess(
            konanc.absolutePath,
            *sourcePaths.map { BASE_DIR.resolve(it).absolutePath }.toTypedArray(),
            *args.toTypedArray<String>()
        ) {
            timeout = konancTimeout
        }
    }

    @Test
    fun testKT72356PassWhenDifferentSourceRange() {
        val runProcessResult = compileLibrary("A.kt", "C.kt", "DEDifferentSourceRange.kt")
        assertEquals("", runProcessResult.output, "Wrong output")
    }

    // Reproducer for KT-72356
    @Test
    fun testKT72356FailWhenDifferentSourceRange() {
        val runProcessResult = try {
            compileLibrary("A.kt", "C.kt", "DESameSourceRange.kt")
        } catch (e: RunProcessException) {
            assertTrue(e.stderr.contains("error: compilation failed: java.lang.IllegalStateException: Cannot serialize annotation @R|Something|()"))
            return
        }
        // TODO: KT-72356: Fix the issue with `FirExpression.toConstantValue()`, correct testcase name, remove try block, remove `assertFalse` below
        assertFalse(false, "RunProcessException was expected as reproducer for KT-72356")
        assertEquals("", runProcessResult.output, "Wrong output")
    }
}