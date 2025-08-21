/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.config.nativeBinaryOptions.GCSchedulerType
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.generateTestCaseWithSingleModule
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Binaries
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.GCScheduler
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.testProcessExecutor
import org.jetbrains.kotlin.konan.test.blackbox.targets
import org.jetbrains.kotlin.native.executors.NoOpExecutor
import org.jetbrains.kotlin.native.executors.RunProcessResult
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.*
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Tag("driver")
@TestDataPath("\$PROJECT_ROOT")
class KonanDriverTest : AbstractNativeSimpleTest() {
    private val konanHome get() = testRunSettings.get<KotlinNativeHome>().dir
    private val buildDir get() = testRunSettings.get<Binaries>().testBinariesDir
    private val konanc get() = konanHome.resolve("bin").resolve(if (HostManager.hostIsMingw) "konanc.bat" else "konanc")
    private val konancTimeout = 15.minutes

    private val testSuiteDir = File("native/native.tests/driver/testData")
    private val source = testSuiteDir.resolve("driver0.kt")
    private val fileRC = testSuiteDir.resolve("File.rc")

    // Driver tests are flaky on macOS hosts.
    private fun `check for KT-67454`() {
        Assumptions.assumeFalse(targets.hostTarget.family.isAppleFamily)
    }

    @BeforeEach
    fun checkAssumptions() {
        `check for KT-67454`()
    }

    @Test
    fun testLLVMVariantDev() {
        // On macOS for apple targets, clang++ from Xcode is used, which is not switchable as `dev/user`,
        // so the test cannot detect LLVM variant for apple targets on macOS host.
        Assumptions.assumeFalse(targets.hostTarget.family.isAppleFamily && targets.testTarget.family.isAppleFamily)
        // No need to test with different GC schedulers
        Assumptions.assumeFalse(testRunSettings.get<GCScheduler>().scheduler == GCSchedulerType.AGGRESSIVE)

        compileSimpleFile(listOf("-Xllvm-variant=dev", "-Xverbose-phases=ObjectFiles")).let {
            assertFalse(
                it.stderr.contains("-essentials"),
                "`-essentials` must not be in stdout of dev LLVM.\nSTDOUT: ${it.stdout}\nSTDERR: ${it.stderr}\n---"
            )
        }
        compileSimpleFile(listOf("-Xllvm-variant=user", "-Xverbose-phases=ObjectFiles")).let {
            assertTrue(
                it.stderr.contains("-essentials"),
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
            timeout = konancTimeout
        }
        testRunSettings.testProcessExecutor.runProcess(kexe.absolutePath) // run generated executable just to check its sanity
        return compilationResult
    }

    @Test
    fun testDriverProducesRunnableBinaries() {
        Assumptions.assumeFalse(HostManager.hostIsMingw &&
            testRunSettings.get<OptimizationMode>() == OptimizationMode.DEBUG
        ) // KT-65963

        val testCase = generateTestCaseWithSingleModule(sourcesRoot = null)
        val kexe = buildDir.resolve("kexe.kexe").also { it.delete() }
        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = TestCompilerArgs.EMPTY,
            sourceModules = testCase.modules,
            extras = TestCase.NoTestRunnerExtras("main"),
            dependencies = emptyList(),
            expectedArtifact = TestCompilationArtifact.Executable(kexe),
            tryPassSystemCacheDirectory = true
        )
        runProcess(konanc.absolutePath, source.absolutePath, *compilation.getCompilerArgs()) {
            timeout = konancTimeout
        }
        val runResult: RunProcessResult = with(testRunSettings) {
            testProcessExecutor.runProcess(kexe.absolutePath) {
                timeout = Duration.parse("1m")
            }
        }
        Assumptions.assumeFalse(testRunSettings.testProcessExecutor is NoOpExecutor) // no output in that case.
        assertEquals("Hello, world!", runResult.stdout)
    }

    @Test
    fun testDriverVersion() {
        Assumptions.assumeFalse(HostManager.hostIsMingw &&
                                        testRunSettings.get<OptimizationMode>() == OptimizationMode.DEBUG
        ) // KT-65963
        // No need to test with different GC schedulers
        Assumptions.assumeFalse(testRunSettings.get<GCScheduler>().scheduler == GCSchedulerType.AGGRESSIVE)

        val testCase = generateTestCaseWithSingleModule(sourcesRoot = null)
        val kexe = buildDir.resolve("kexe.kexe").also { it.delete() }
        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = TestCompilerArgs(listOf("-version")),
            sourceModules = testCase.modules,
            extras = TestCase.NoTestRunnerExtras("main"),
            dependencies = emptyList(),
            expectedArtifact = TestCompilationArtifact.Executable(kexe),
            tryPassSystemCacheDirectory = true
        )
        runProcess(konanc.absolutePath, source.absolutePath, *compilation.getCompilerArgs()) {
            timeout = konancTimeout
        }
        assertFalse(kexe.exists())
    }

    @Test
    fun testOverrideKonanProperties() {
        // Only test with -opt enabled
        Assumptions.assumeTrue(testRunSettings.get<OptimizationMode>() == OptimizationMode.OPT)
        // No need to test with different GC schedulers
        Assumptions.assumeFalse(testRunSettings.get<GCScheduler>().scheduler == GCSchedulerType.AGGRESSIVE)

        val testCase = generateTestCaseWithSingleModule(sourcesRoot = null)
        val kexe = buildDir.resolve("kexe.kexe").also { it.delete() }
        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xverbose-phases=MandatoryBitcodeLLVMPostprocessingPhase",
                    "-Xoverride-konan-properties=llvmInlineThreshold=76",
                )
            ),
            sourceModules = testCase.modules,
            extras = TestCase.NoTestRunnerExtras("main"),
            dependencies = emptyList(),
            expectedArtifact = TestCompilationArtifact.Executable(kexe),
            tryPassSystemCacheDirectory = true
        )
        val compilationResult = runProcess(konanc.absolutePath, source.absolutePath, *compilation.getCompilerArgs()) {
            timeout = konancTimeout
        }
        val expected = "inline_threshold: 76"
        assertTrue(
            compilationResult.stderr.contains(expected),
            "Compiler's stderr must contain string: $expected\n" +
                    "STDOUT:\n${compilationResult.stdout}\nSTDERR:${compilationResult.stderr}"
        )
        testRunSettings.testProcessExecutor.runProcess(kexe.absolutePath)
    }

    @Disabled("The test is not working on Windows Server 2019-based TeamCity agents for the unknown reason." +
                "KT-49279: TODO: Try re-enable test after LLVM update >=13.0.0 where llvm-windres was added")
    @Test
    fun testKT50983() {
        Assumptions.assumeTrue(HostManager.hostIsMingw)

        val windres = File(testRunSettings.configurables.absoluteTargetToolchain).resolve("bin").resolve("windres")
        val fileRes = buildDir.resolve("File.res")
        runProcess(
            windres.absolutePath,
            fileRC.absolutePath, "-O", "coff", "-o", fileRes.absolutePath,
            "--use-temp-file", // https://github.com/msys2/MINGW-packages/issues/534
            // https://sourceforge.net/p/mingw-w64/discussion/723798/thread/bf2a464d/
            "--preprocessor=x86_64-w64-mingw32-g++.exe",
            "--preprocessor-arg=-E", "--preprocessor-arg=-xc-header", "--preprocessor-arg=-DRC_INVOKED",
        ) {
            timeout = Duration.parse("1m")
        }

        val module = TestModule.Exclusive("moduleName", emptySet(), emptySet(), emptySet())
        val kexe = buildDir.resolve("kexe.kexe").also { it.delete() }
        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = TestCompilerArgs(listOf("-linker-option", fileRes.absolutePath)),
            sourceModules = listOf(module),
            extras = TestCase.NoTestRunnerExtras("main"),
            dependencies = emptyList(),
            expectedArtifact = TestCompilationArtifact.Executable(kexe),
            tryPassSystemCacheDirectory = true
        )
        runProcess(konanc.absolutePath, source.absolutePath, *compilation.getCompilerArgs()) {
            timeout = Duration.parse("5m")
        }
        testRunSettings.testProcessExecutor.runProcess(kexe.absolutePath)
    }

    @Test
    fun testSplitCompilationPipeline() {

        val rootDir = testSuiteDir.resolve("split_compilation_pipeline")
        val libFile = buildDir.resolve("lib.klib")
        runProcess(
            konanc.absolutePath,
            rootDir.resolve("override_lib.kt").absolutePath,
            "-produce", "library",
            "-o", libFile.absolutePath,
            "-target", targets.testTarget.visibleName,
        ) {
            timeout = konancTimeout
        }

        val tmpFilesDir = buildDir.resolve("tmpFiles").apply {
            deleteRecursively()
            mkdirs()
        }
        val depsFile = buildDir.resolve("deps.deps")

        val mainFile = buildDir.resolve("out.klib")
        runProcess(
            konanc.absolutePath,
            rootDir.resolve("override_main.kt").absolutePath,
            "-o", mainFile.absolutePath,
            "-target", targets.testTarget.visibleName,
            "-l", libFile.absolutePath,
            "-Xtemporary-files-dir=${tmpFilesDir.absolutePath}",
            "-Xwrite-dependencies-to=${depsFile.absolutePath}",
        ) {
            timeout = konancTimeout
        }

        val kexe = buildDir.resolve("kexe.kexe").also { it.delete() }
        runProcess(
            konanc.absolutePath,
            rootDir.resolve("override_main.kt").absolutePath,
            "-o", kexe.absolutePath,
            "-target", targets.testTarget.visibleName,
            "-Xread-dependencies-from=${depsFile.absolutePath}",
            "-Xcompile-from-bitcode=${tmpFilesDir.absolutePath}/out.bc"
        )
        val output = testRunSettings.testProcessExecutor.runProcess(kexe.absolutePath).output
        Assumptions.assumeFalse(testRunSettings.testProcessExecutor is NoOpExecutor) // no output in that case.
        KotlinTestUtils.assertEqualsToFile(rootDir.resolve("override_main.out"), output)
    }

    @Test
    fun testSplitCompilationPipelineWithKlibResolverFlags() {
        Assumptions.assumeFalse(HostManager.hostIsMingw &&
                                        testRunSettings.get<OptimizationMode>() == OptimizationMode.DEBUG
        ) // KT-65963

        val rootDir = testSuiteDir.resolve("split_compilation_pipeline")
        val libFile = buildDir.resolve("lib.klib")
        runProcess(
            konanc.absolutePath,
            rootDir.resolve("override_lib.kt").absolutePath,
            "-produce", "library",
            "-o", libFile.absolutePath,
            "-target", targets.testTarget.visibleName,
        ) {
            timeout = konancTimeout
        }

        val libFile2 = buildDir.resolve("lib2.klib")
        File(libFile.absolutePath).copyRecursively(libFile2)

        val mainFile = buildDir.resolve("out.klib")
        runProcess(
            konanc.absolutePath,
            rootDir.resolve("override_main.kt").absolutePath,
            "-o", mainFile.absolutePath,
            "-target", targets.testTarget.visibleName,
            "-l", libFile.absolutePath,
            "-l", libFile2.absolutePath,
            "-Xklib-duplicated-unique-name-strategy=allow-all-with-warning",
        ) {
            timeout = konancTimeout
        }.let {
            assertTrue(
                it.stderr.contains("warning: KLIB resolver: The same 'unique_name=lib' found in more than one library"),
                "`warning: KLIB resolver: The same 'unique_name=lib' found in more than one library` must be in stdout." +
                        "\nSTDOUT: ${it.stdout}\nSTDERR: ${it.stderr}\n---"
            )
        }
    }

    @Test
    fun noSourcesOrIncludeKlib() {
        val rootDir = testSuiteDir.resolve("kt68673")
        val libFile = buildDir.resolve("program.klib")
        val kexe = buildDir.resolve("program.kexe")
        runProcess(
            konanc.absolutePath,
            rootDir.resolve("main.kt").absolutePath,
            "-produce", "library",
            "-o", libFile.absolutePath,
            "-target", targets.testTarget.visibleName,
        ) {
            timeout = konancTimeout
        }

        runProcess(
            konanc.absolutePath,
            "-l", libFile.absolutePath,
            "-o", kexe.absolutePath,
            "-target", targets.testTarget.visibleName,
            "-g"
        ) {
            timeout = konancTimeout
        }
        val output = testRunSettings.testProcessExecutor.runProcess(kexe.absolutePath).output
        Assumptions.assumeFalse(testRunSettings.testProcessExecutor is NoOpExecutor) // no output in that case.
        KotlinTestUtils.assertEqualsToFile(rootDir.resolve("main.out"), output)
    }
}
