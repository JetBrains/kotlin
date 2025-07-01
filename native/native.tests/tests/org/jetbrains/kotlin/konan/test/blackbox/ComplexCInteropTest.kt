/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.config.nativeBinaryOptions.GC
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.isSimulator
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.ClassicPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClang
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClangToStaticLibrary
import org.jetbrains.kotlin.native.executors.RunProcessResult
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ClassicPipeline()
@TestDataPath("\$PROJECT_ROOT")
class ClassicComplexCInteropTest : ComplexCInteropTestBase()

@TestDataPath("\$PROJECT_ROOT")
class FirComplexCInteropTest : ComplexCInteropTestBase()

abstract class ComplexCInteropTestBase : AbstractNativeSimpleTest() {
    private val interopDir = File("native/native.tests/testData/interop")
    private val interopObjCDir = interopDir.resolve("objc")
    private val testCompilationFactory = TestCompilationFactory()

    @Test
    @TestMetadata("embedStaticLibraries.kt")
    fun testEmbedStaticLibraries() {
        val embedStaticLibrariesDir = interopDir.resolve("embedStaticLibraries")
        (1..4).forEach {
            val source = embedStaticLibrariesDir.resolve("$it.c")
            val libFile = buildDir.resolve("$it.a")
            compileWithClangToStaticLibrary(
                sourceFiles = listOf(source),
                outputFile = libFile,
            ).assertSuccess()
        }
        val defFile = buildDir.resolve("embedStaticLibraries.def").also {
            it.printWriter().use {
                it.println(
                    """
                    libraryPaths = ${buildDir.absolutePath.replace("\\", "/")}
                    staticLibraries = 3.a 4.a
                """.trimIndent()
                )
            }
        }
        val (testCase, compilationResult) = compileDefAndKtToExecutable(
            testName = "embedStaticLibraries",
            defFile = defFile,
            ktFiles = listOf(embedStaticLibrariesDir.resolve("embedStaticLibraries.kt")),
            freeCompilerArgs = TestCompilerArgs(
                compilerArgs = listOf("-opt-in=kotlinx.cinterop.ExperimentalForeignApi"),
                cinteropArgs = listOf(
                    "-header", embedStaticLibrariesDir.resolve("embedStaticLibraries.h").absolutePath,
                    "-staticLibrary", "1.a",
                    "-staticLibrary", "2.a",
                )
            ),
            extras = TestCase.NoTestRunnerExtras("main"),
        )
        val testExecutable = TestExecutable(
            compilationResult.resultingArtifact,
            compilationResult.loggedData,
            listOf(TestName("embedStaticLibraries"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    @Test
    @TestMetadata("smoke.kt")
    fun testInteropObjCSmokeGC() {
        Assumptions.assumeTrue(testRunSettings.get<GCType>().gc != GC.NOOP)
        testInteropObjCSmoke("smoke")
    }

    @Test
    @TestMetadata("smoke_noopgc.kt")
    fun testInteropObjCSmokeNoopGC() {
        Assumptions.assumeTrue(testRunSettings.get<GCType>().gc == GC.NOOP)
        testInteropObjCSmoke("smoke_noopgc")
    }

    private fun testInteropObjCSmoke(ktFilePrefix: String) {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val mSources = listOf(interopObjCDir.resolve("smoke.m"))
        val dylib = compileDylib("objcsmoke", mSources)
        if (testRunSettings.configurables.targetTriple.isSimulator)
            codesign(dylib.resultingArtifact.path)
        val stringsdict = interopObjCDir.resolve("Localizable.stringsdict")
        stringsdict.copyTo(buildDir.resolve("$ktFilePrefix/en.lproj/Localizable.stringsdict"), overwrite = true)

        val (testCase, success) = compileDefAndKtToExecutable(
            testName = ktFilePrefix,
            defFile = interopObjCDir.resolve("objcSmoke.def"),
            ktFiles = listOf(interopObjCDir.resolve("$ktFilePrefix.kt")),
            freeCompilerArgs = TestCompilerArgs(
                compilerArgs = listOf("-opt-in=kotlinx.cinterop.ExperimentalForeignApi", "-linker-option", "-L${buildDir.absolutePath}"),
                cinteropArgs = listOf("-header", "smoke.h")
            ),
            extras = TestCase.NoTestRunnerExtras("main"),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                outputDataFile = TestRunCheck.OutputDataFile(file = interopObjCDir.resolve("$ktFilePrefix.out"))
            ),
        )
        val testExecutable = TestExecutable(
            success.resultingArtifact,
            success.loggedData,
            listOf(TestName("smoke"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    @Test
    @TestMetadata("tests")
    fun testInteropObjCTests() {
        fun fileList(subFolder: String, extension: String) =
            interopObjCDir.resolve(subFolder).walk().filter { it.isFile && it.extension == extension }.toList()

        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val ktFiles = fileList("tests", "kt")
        val hFiles = fileList("tests", "h")
        val mFiles = fileList("tests", "m")
        val dylib = compileDylib("objctests", mFiles)
        if (testRunSettings.configurables.targetTriple.isSimulator)
            codesign(dylib.resultingArtifact.path)

        val ignoredTestGTestPatterns = if (testRunSettings.get<GCType>().gc != GC.NOOP) emptySet() else setOf(
            "Kt41811Kt.*",
            "CustomStringKt.testCustomString",
            "Kt42482Kt.testKT42482",
            "ObjcWeakRefsKt.testObjCWeakRef",
            "WeakRefsKt.testWeakRefs",
        )
        val (testCase, success) = compileDefAndKtToExecutable(
            testName = "embedStaticLibraries",
            defFile = interopObjCDir.resolve("objcTests.def"),
            ktFiles = ktFiles,
            freeCompilerArgs = TestCompilerArgs(
                compilerArgs = listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in=kotlin.native.internal.InternalForKotlinNative",
                    "-XXLanguage:+ImplicitSignedToUnsignedIntegerConversion",
                    "-tr", "-e", "main", "-linker-option", "-L${buildDir.absolutePath}"
                ),
                cinteropArgs = hFiles.flatMap { listOf("-header", "tests/${it.name}") }
            ),
            extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT, ignoredTestGTestPatterns),
        )
        val testExecutable = TestExecutable(
            success.resultingArtifact,
            success.loggedData,
            listOf(TestName("interop_objc_tests"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    @Test
    @TestMetadata("with_initializer")
    fun testObjCWithGlobalInitializer() {
        val execResult = testDylibCinteropExe("with_initializer")
        Assumptions.assumingThat(!testRunSettings.get<ForcedNoopTestRunner>().value) {
            assertEquals("OK", execResult.stdout)
        }
    }

    @Test
    @TestMetadata("messaging")
    fun testMessaging() {
        testDylibCinteropExe("messaging")
    }

    @Test
    @TestMetadata("kt42172")
    fun testKt42172() {
        Assumptions.assumeFalse(testRunSettings.get<GCType>().gc != GC.NOOP)
        val execResult = testDylibCinteropExe("kt42172")
        Assumptions.assumingThat(!testRunSettings.get<ForcedNoopTestRunner>().value) {
            assertEquals("Executed finalizer", execResult.stdout)
        }
    }

    @Test
    @TestMetadata("include_categories")
    fun testInclude_categories() {
        val execResult = testDylibCinteropExe("include_categories")
        Assumptions.assumingThat(!testRunSettings.get<ForcedNoopTestRunner>().value) {
            assertEquals(
                """
                3.0
                3.14
                6.0
                
                3
                6
                6.0
                
                3.0
                600.0
                """.trimIndent(), execResult.stdout
            )
        }
    }

    @Test
    @TestMetadata("kt56402")
    fun testKt56402() {
        // Test depends on macOS-specific AppKit and some GC
        Assumptions.assumeTrue(targets.testTarget.family == Family.OSX)
        Assumptions.assumeFalse(testRunSettings.get<GCType>().gc == GC.NOOP)
        Assumptions.assumeFalse(testRunSettings.get<CacheMode>().useStaticCacheForUserLibraries) // KT-66032: -tr is incompatible with caches
        val execResult = testDylibCinteropExe(
            "kt56402",
            extraClangOpts = listOf("-framework", "AppKit", "-fobjc-arc"),
            extraCompilerOpts = listOf("-tr"),
        )
        Assumptions.assumingThat(!testRunSettings.get<ForcedNoopTestRunner>().value) {
            assertTrue(execResult.stdout.contains("[  PASSED  ] 8 tests"), execResult.stdout)
        }
    }

    @Test
    @TestMetadata("friendly_dealloc")
    fun testFriendly_dealloc() {
        Assumptions.assumeFalse(testRunSettings.get<GCType>().gc != GC.NOOP)
        val execResult = testDylibCinteropExe(
            "friendly_dealloc",
            extraClangOpts = listOf("-fno-objc-arc"),
            extraCompilerOpts = listOf("-tr", "-XXLanguage:+ImplicitSignedToUnsignedIntegerConversion"),
            extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT),
        )
        Assumptions.assumingThat(!testRunSettings.get<ForcedNoopTestRunner>().value) {
            assertTrue(execResult.stdout.contains("[  PASSED  ] 4 tests"))
        }
    }

    @Test
    @TestMetadata("objCAction")
    fun testObjCAction() {
        val execResult = testDylibCinteropExe(
            "objCAction",
            extraCompilerOpts = listOf("-tr"),
            extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT),
        )
        Assumptions.assumingThat(!testRunSettings.get<ForcedNoopTestRunner>().value) {
            assertTrue(execResult.stdout.contains("[  PASSED  ] 5 tests"))
        }
    }

    @Test
    @TestMetadata("foreignException")
    fun testForeignException() {
        val res = testDylibCinteropExe(
            "foreignException",
            "objc_wrap.h",
            "objc_wrap.m",
            "objc_wrap.def",
            "objc_wrap.kt",
        )
        Assumptions.assumingThat(!testRunSettings.get<ForcedNoopTestRunner>().value) {
            assertEquals("", res.stdout)
            assertEquals("", res.stderr)
        }
    }

    @Test
    @TestMetadata("foreignException")
    fun testForeignExceptionMode_default() {
        val res = testDylibCinteropExe(
            "foreignException",
            "objc_wrap.h",
            "objc_wrap.m",
            "objcExceptionMode.def",
            "objcExceptionMode_wrap.kt",
        )
        Assumptions.assumingThat(!testRunSettings.get<ForcedNoopTestRunner>().value) {
            assertEquals("OK: Ends with uncaught exception handler", res.stdout)
            assertEquals("", res.stderr)
        }
    }

    @Test
    @TestMetadata("foreignException")
    fun testForeignExceptionMode_wrap() {
        val res = testDylibCinteropExe(
            "foreignException",
            "objc_wrap.h",
            "objc_wrap.m",
            "objcExceptionMode.def",
            "objcExceptionMode_wrap.kt",
            extraCinteropArgs = listOf("-Xforeign-exception-mode", "objc-wrap")
        )
        Assumptions.assumingThat(!testRunSettings.get<ForcedNoopTestRunner>().value) {
            assertEquals("OK: ForeignException", res.stdout)
            assertEquals("", res.stderr)
        }
    }

    private fun testDylibCinteropExe(
        testName: String,
        extraClangOpts: List<String> = listOf("-fobjc-arc"),
        extraCompilerOpts: List<String> = emptyList(),
        extras: TestCase.Extras = TestCase.NoTestRunnerExtras("main"),
    ): RunProcessResult {
        return testDylibCinteropExe(
            testName,
            hFile = "$testName.h",
            mFile = "$testName.m",
            defFile = "$testName.def",
            ktFile = "$testName.kt",
            extraClangOpts = extraClangOpts,
            extraCompilerOpts = extraCompilerOpts,
            extras = extras,
        )
    }

    private fun testDylibCinteropExe(
        testName: String,
        hFile: String,
        mFile: String,
        defFile: String,
        ktFile: String,
        extraCinteropArgs: List<String> = emptyList(),
        extraClangOpts: List<String> = listOf("-fobjc-arc"),
        extraCompilerOpts: List<String> = emptyList(),
        extras: TestCase.Extras = TestCase.NoTestRunnerExtras("main"),
    ): RunProcessResult {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val srcDir = interopObjCDir.resolve(testName)
        val dylib = compileDylib(testName, listOf(srcDir.resolve(mFile)), extraClangOpts)
        if (testRunSettings.configurables.targetTriple.isSimulator)
            codesign(dylib.resultingArtifact.path)

        val (_, success) = compileDefAndKtToExecutable(
            testName = testName,
            defFile = srcDir.resolve(defFile),
            ktFiles = listOf(srcDir.resolve(ktFile)),
            freeCompilerArgs = TestCompilerArgs(
                compilerArgs = listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-opt-in=kotlin.native.internal.InternalForKotlinNative",
                    "-linker-option", "-L${buildDir.absolutePath}",
                    *extraCompilerOpts.toTypedArray<String>()
                ),
                cinteropArgs = extraCinteropArgs + listOf("-header", srcDir.resolve(hFile).absolutePath)
            ),
            extras = extras,
        )
        return testRunSettings.testProcessExecutor.runProcess(success.resultingArtifact.executableFile.absolutePath)
    }

    @Test
    @TestMetadata("withSpaces.kt")
    fun testWithSpaces() {
        val srcDir = interopDir.resolve("withSpaces")
        val mapfile = buildDir.resolve("cutom map.map").also { it.delete() }
        val mapOption = when {
            targets.testTarget.family.isAppleFamily -> "-map \"${buildDir.absolutePath}/cutom map.map\""
            targets.testTarget.family == Family.MINGW -> "\"-Wl,--Map,${buildDir.absolutePath}/cutom map.map\""
            else -> "--Map \"${buildDir.absolutePath}/cutom map.map\""
        }
        val withSpacesDef = buildDir.resolve("withSpaces.def").also {
            it.printWriter().use {
                it.println(
                    """
                    headers = stdio.h "${srcDir.absolutePath}/custom headers/custom.h"
                    linkerOpts = $mapOption
                    ---
    
                    int customCompare(const char* str1, const char* str2) {
                        return custom_strcmp(str1, str2);
                    }
                """.trimIndent()
                )
            }
        }

        val (testCase, success) = compileDefAndKtToExecutable(
            testName = "withSpaces",
            defFile = withSpacesDef,
            ktFiles = listOf(srcDir.resolve("withSpaces.kt")),
            freeCompilerArgs = TestCompilerArgs("-opt-in=kotlinx.cinterop.ExperimentalForeignApi"),
            extras = TestCase.NoTestRunnerExtras("main"),
        )

        val testExecutable = TestExecutable(
            success.resultingArtifact,
            success.loggedData,
            listOf(TestName("interop_objc_tests"))
        )
        runExecutableAndVerify(testCase, testExecutable)
        assertTrue(mapfile.exists())
    }

    private fun compileDefAndKtToExecutable(
        testName: String,
        defFile: File,
        ktFiles: Collection<File>,
        freeCompilerArgs: TestCompilerArgs,
        extras: TestCase.Extras,
        checks: TestRunChecks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
    ): Pair<TestCase, TestCompilationResult.Success<out TestCompilationArtifact.Executable>> {
        val cinteropModule = TestModule.Exclusive("cinterop", emptySet(), emptySet(), emptySet())
        cinteropModule.files += TestFile.createCommitted(defFile, cinteropModule)

        val ktModule = TestModule.Exclusive("main", setOf("cinterop"), emptySet(), emptySet())
        ktFiles.forEach { ktModule.files += TestFile.createCommitted(it, ktModule) }

        val testKind = when (extras) {
            is TestCase.NoTestRunnerExtras -> TestKind.STANDALONE_NO_TR
            is TestCase.WithTestRunnerExtras -> TestKind.STANDALONE
        }
        val testCase = TestCase(
            id = TestCaseId.Named(testName),
            kind = testKind,
            modules = setOf(cinteropModule, ktModule),
            freeCompilerArgs = freeCompilerArgs,
            nominalPackageName = PackageName(testName),
            checks = checks,
            extras = extras
        ).apply {
            initialize(null, null)
        }
        // Compile test, respecting possible `mode=TWO_STAGE_MULTI_MODULE`: add intermediate LibraryCompilation(kt->klib), if needed.
        val success = testCompilationFactory.testCasesToExecutable(listOf(testCase), testRunSettings).result.assertSuccess()
        return Pair(testCase, success)
    }

    private fun compileDylib(name: String, mSources: List<File>, extraClangOpts: List<String> = listOf("-fobjc-arc")): TestCompilationResult.Success<out TestCompilationArtifact.Executable> {
        val clangResult = compileWithClang(
            sourceFiles = mSources,
            includeDirectories = emptyList(),
            outputFile = buildDir.resolve("lib$name.dylib"),
            libraryDirectories = emptyList(),
            libraries = emptyList(),
            additionalClangFlags = extraClangOpts + listOf(
                "-DNS_FORMAT_ARGUMENT(A)=", "-lobjc", "-fPIC", "-shared",
                // Enable ARC optimizations to prevent some objects from leaking in Obj-C code due to exceptions:
                "-O2"
            ),
            fmodules = false, // with `-fmodules`, ld cannot find symbol `_assert`
        ).assertSuccess()
        return clangResult
    }

    @Test
    @TestMetadata("arc_contract")
    fun arcContract() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        Assumptions.assumeTrue(targets.testTarget.architecture == Architecture.ARM64)
        Assumptions.assumeTrue(targets.testTarget != KonanTarget.WATCHOS_ARM64) // Not real ARM64.
        val root = interopObjCDir.resolve("arc_contract")
        val bcFile = buildDir.resolve("arc_contract.bc")
        runProcess(
            "${testRunSettings.configurables.absoluteLlvmHome}/bin/llvm-as",
            root.resolve("main.ll").absolutePath,
            "-o",
            bcFile.absolutePath
        )
        val testCase = generateTestCaseWithSingleFile(
            root.resolve("main.kt"),
            testKind = TestKind.STANDALONE_NO_TR,
            extras = TestCase.NoTestRunnerExtras(),
            freeCompilerArgs = TestCompilerArgs("-native-library", bcFile.absolutePath),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                outputDataFile = TestRunCheck.OutputDataFile(file = root.resolve("main.out"))
            )
        )
        val compilationResult = compileToExecutableInOneStage(testCase).assertSuccess()
        runExecutableAndVerify(testCase, TestExecutable.fromCompilationResult(testCase, compilationResult))
    }

    @Test
    @TestMetadata("safepointSignposts")
    fun safepointSignposts() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        Assumptions.assumeFalse(targets.areDifferentTargets())

        val srcDir = interopObjCDir.resolve("safepointSignposts")
        compileDylib("cinterop", listOf(srcDir.resolve("cinterop.m")))

        val (testCase, compilationResult) = compileDefAndKtToExecutable(
            testName = "safepointSignposts",
            defFile = srcDir.resolve("cinterop.def"),
            ktFiles = listOf(srcDir.resolve("main.kt")),
            freeCompilerArgs = TestCompilerArgs(
                compilerArgs = listOf(
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                    "-linker-option", "-L${buildDir.absolutePath}",
                )
            ),
            extras = TestCase.NoTestRunnerExtras(),
        )
        val process = ProcessBuilder("/usr/bin/log", "stream", "--signpost").start()
        try {
            runExecutableAndVerify(testCase, TestExecutable.fromCompilationResult(testCase, compilationResult))
        } finally {
            process.destroyForcibly()
        }
    }

    @Test
    @TestMetadata("initWithExternalRCRef_leak")
    fun testInitWithExternalRCRefLeak() {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        Assumptions.assumeFalse(targets.areDifferentTargets())

        val srcDir = interopDir.resolve("swift/initWithExternalRCRef_leak")

        val (testCase, compilationResult) = compileDefAndKtToExecutable(
            testName = "initWithExtgernalRCRef_leak",
            defFile = srcDir.resolve("cinterop.def"),
            ktFiles = listOf(srcDir.resolve("main.kt")),
            freeCompilerArgs = TestCompilerArgs(
                compilerArgs = listOf(
                    "-opt-in=kotlin.native.internal.InternalForKotlinNative",
                    "-Xbinary=swiftExport=true",
                ),
                cinteropArgs = listOf("-Xcompile-source", srcDir.resolve("cinterop.m").path),
                objcArc = false
            ),
            extras = TestCase.NoTestRunnerExtras(),
        )
        runExecutableAndVerify(testCase, TestExecutable.fromCompilationResult(testCase, compilationResult))
    }
}