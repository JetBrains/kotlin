/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeSimpleTestSupport
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestModule
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Binaries
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.GCScheduler
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.executor
import org.jetbrains.kotlin.konan.test.blackbox.targets
import org.jetbrains.kotlin.native.executors.RunProcessResult
import org.jetbrains.kotlin.native.executors.runProcess
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration

@Tag("driver")
@TestDataPath("\$PROJECT_ROOT")
@ExtendWith(NativeSimpleTestSupport::class)
class KonanDriverTest : AbstractNativeSimpleTest() {
    private val konanHome get() = testRunSettings.get<KotlinNativeHome>().dir
    private val buildDir get() = testRunSettings.get<Binaries>().testBinariesDir
    private val konanc get() = konanHome.resolve("bin").resolve(if (HostManager.hostIsMingw) "konanc.bat" else "konanc")

    private val testSuiteDir = File("native/native.tests/testData/driver")
    private val source = testSuiteDir.resolve("driver0.kt")

    @Test
    fun testLLVMVariantDev() {
        // On macOS for apple targets, clang++ from Xcode is used, which is not switchable as `dev/user`,
        // so the test cannot detect LLVM variant for apple targets on macOS host.
        Assumptions.assumeFalse(targets.hostTarget.family.isAppleFamily && targets.testTarget.family.isAppleFamily)
        // No need to test with different GC schedulers
        Assumptions.assumeFalse(testRunSettings.get<GCScheduler>() == GCScheduler.AGGRESSIVE)

        compileSimpleFile(listOf("-Xllvm-variant=dev", "-Xverbose-phases=ObjectFiles")).let {
            assertFalse(
                it.stdout.contains("-essentials"),
                "`-essentials` must not be in stdout of dev LLVM.\nSTDOUT: ${it.stdout}\nSTDERR: ${it.stderr}\n---"
            )
        }
        compileSimpleFile(listOf("-Xllvm-variant=user", "-Xverbose-phases=ObjectFiles")).let {
            assertTrue(
                it.stdout.contains("-essentials"),
                "`-essentials` must be in stdout of user LLVM.\nSTDOUT: ${it.stdout}\nSTDERR: ${it.stderr}\n---"
            )
        }
    }

    private fun compileSimpleFile(flags: List<String>): RunProcessResult {
        val kexe = buildDir.resolve("kexe.kexe").also { it.delete() }
        val args = mutableListOf("-output", kexe.absolutePath).apply {
            add("-target")
            add(targets.testTarget.visibleName)
            addAll(flags)

            add("-Xpartial-linkage=enable")
            add("-Xpartial-linkage-loglevel=error")
        }

        val compilationResult = runProcess(konanc.absolutePath, source.absolutePath, *args.toTypedArray<String>()) {
            timeout = Duration.parse("5m")
        }
        testRunSettings.executor.runProcess(kexe.absolutePath) // run generated executable just to check its sanity
        return compilationResult
    }

    @Test
    fun testDriverProducesRunnableBinaries() {
        Assumptions.assumeFalse(HostManager.hostIsMingw &&
            testRunSettings.get<CacheMode>() == CacheMode.WithoutCache &&
            testRunSettings.get<OptimizationMode>() == OptimizationMode.DEBUG
        ) // KT-65963
        val module = TestModule.Exclusive("moduleName", emptySet(), emptySet(), emptySet())
        val kexe = buildDir.resolve("kexe.kexe").also { it.delete() }
        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = TestCompilerArgs.EMPTY,
            sourceModules = listOf(module),
            extras = TestCase.NoTestRunnerExtras("main"),
            dependencies = emptyList(),
            expectedArtifact = TestCompilationArtifact.Executable(kexe),
            tryPassSystemCacheDirectory = true
        )
        runProcess(konanc.absolutePath, source.absolutePath, *compilation.getCompilerArgs()) {
            timeout = Duration.parse("5m")
        }
        val runResult: RunProcessResult = with(testRunSettings) {
            executor.runProcess(kexe.absolutePath) {
                timeout = Duration.parse("1m")
            }
        }
        assertEquals("Hello, world!", runResult.stdout)
    }

    @Test
    fun testDriverVersion() {
        Assumptions.assumeFalse(HostManager.hostIsMingw &&
                                        testRunSettings.get<CacheMode>() == CacheMode.WithoutCache &&
                                        testRunSettings.get<OptimizationMode>() == OptimizationMode.DEBUG
        ) // KT-65963
        // No need to test with different GC schedulers
        Assumptions.assumeFalse(testRunSettings.get<GCScheduler>() == GCScheduler.AGGRESSIVE)

        val module = TestModule.Exclusive("moduleName", emptySet(), emptySet(), emptySet())
        val kexe = buildDir.resolve("kexe.kexe").also { it.delete() }
        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = TestCompilerArgs(listOf("-version")),
            sourceModules = listOf(module),
            extras = TestCase.NoTestRunnerExtras("main"),
            dependencies = emptyList(),
            expectedArtifact = TestCompilationArtifact.Executable(kexe),
            tryPassSystemCacheDirectory = true
        )
        runProcess(konanc.absolutePath, source.absolutePath, *compilation.getCompilerArgs()) {
            timeout = Duration.parse("5m")
        }
        assertFalse(kexe.exists())
    }

    @Test
    fun testOverrideKonanProperties() {
        Assumptions.assumeFalse(HostManager.hostIsMingw &&
                                        testRunSettings.get<CacheMode>() == CacheMode.WithoutCache &&
                                        testRunSettings.get<OptimizationMode>() == OptimizationMode.DEBUG
        ) // KT-65963
        // No need to test with different GC schedulers
        Assumptions.assumeFalse(testRunSettings.get<GCScheduler>() == GCScheduler.AGGRESSIVE)

        val module = TestModule.Exclusive("moduleName", emptySet(), emptySet(), emptySet())
        val kexe = buildDir.resolve("kexe.kexe").also { it.delete() }
        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-opt",
                    "-Xverbose-phases=MandatoryBitcodeLLVMPostprocessingPhase",
                    if (HostManager.hostIsMingw)
                        "-Xoverride-konan-properties=\"llvmInlineThreshold=76\""
                    else "-Xoverride-konan-properties=llvmInlineThreshold=76"
                )),
            sourceModules = listOf(module),
            extras = TestCase.NoTestRunnerExtras("main"),
            dependencies = emptyList(),
            expectedArtifact = TestCompilationArtifact.Executable(kexe),
            tryPassSystemCacheDirectory = true
        )
        val compilationResult = runProcess(konanc.absolutePath, source.absolutePath, *compilation.getCompilerArgs()) {
            timeout = Duration.parse("5m")
        }
        val expected = "inline_threshold: 76"
        assertTrue(
            compilationResult.stdout.contains(expected),
            "Compiler's stdout must contain string: $expected\n" +
                    "STDOUT:\n${compilationResult.stdout}\nSTDERR:${compilationResult.stderr}"
        )
        testRunSettings.executor.runProcess(kexe.absolutePath)
    }
}
