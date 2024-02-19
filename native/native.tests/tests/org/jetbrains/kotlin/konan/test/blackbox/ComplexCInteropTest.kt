/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.target.ClangArgs
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.isSimulator
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.ClangDistribution
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClang
import org.jetbrains.kotlin.native.executors.RunProcessResult
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration

@TestDataPath("\$PROJECT_ROOT")
class ClassicComplexCInteropTest : ComplexCInteropTestBase()

@FirPipeline
@Tag("frontend-fir")
@TestDataPath("\$PROJECT_ROOT")
class FirComplexCInteropTest : ComplexCInteropTestBase()

abstract class ComplexCInteropTestBase : AbstractNativeSimpleTest() {
    private val interopDir = File("native/native.tests/testData/interop")
    private val interopObjCDir = interopDir.resolve("objc")

    @Test
    @TestMetadata("embedStaticLibraries.kt")
    fun testEmbedStaticLibraries() {
        val llvmAr = ClangArgs.Native(testRunSettings.configurables).llvmAr().first()
        val embedStaticLibrariesDir = interopDir.resolve("embedStaticLibraries")
        (1..4).forEach {
            val source = embedStaticLibrariesDir.resolve("$it.c")
            val obj = buildDir.resolve("$it.o")
            compileWithClang(
                clangDistribution = ClangDistribution.Llvm,
                sourceFiles = listOf(source),
                includeDirectories = emptyList(),
                outputFile = obj,
                libraryDirectories = emptyList(),
                libraries = emptyList(),
                additionalClangFlags = listOf("-c"),
                fmodules = false, // with `-fmodules`, ld cannot find symbol `_assert`
            ).assertSuccess()
            val libFile = buildDir.resolve("$it.a")
            runProcess(llvmAr, "-rc", libFile.absolutePath, obj.absolutePath) {
                timeout = Duration.parse("1m")
            }
            assertTrue(libFile.exists())
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
        val cinteropKlib = cinteropToLibrary(
            targets = targets,
            defFile = defFile,
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs(
                emptyList(), cinteropArgs = listOf(
                    "-header", embedStaticLibrariesDir.resolve("embedStaticLibraries.h").absolutePath,
                    "-staticLibrary", "1.a",
                    "-staticLibrary", "2.a",
                )
            )
        ).assertSuccess().resultingArtifact

        val testCase = generateTestCaseWithSingleFile(
            sourceFile = embedStaticLibrariesDir.resolve("embedStaticLibraries.kt"),
            testKind = TestKind.STANDALONE_NO_TR,
            extras = TestCase.NoTestRunnerExtras("main"),
            freeCompilerArgs = TestCompilerArgs("-opt-in=kotlinx.cinterop.ExperimentalForeignApi"),
        )
        val compilationResult = compileToExecutable(testCase, cinteropKlib.asLibraryDependency()).assertSuccess()
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
        Assumptions.assumeTrue(
            testRunSettings.get<GCType>() != GCType.NOOP
                    || testRunSettings.get<CacheMode>() != CacheMode.WithoutCache // TODO: Remove line after fix of KT-63944
        )
        testInteropObjCSmoke("smoke")
    }

    @Test
    @TestMetadata("smoke_noopgc.kt")
    fun testInteropObjCSmokeNoopGC() {
        Assumptions.assumeTrue(testRunSettings.get<GCType>() == GCType.NOOP)
        Assumptions.assumeTrue(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache) // TODO: Remove line after fix of KT-63944
        testInteropObjCSmoke("smoke_noopgc")
    }

    private fun testInteropObjCSmoke(ktFilePrefix: String) {
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val mSources = listOf(interopObjCDir.resolve("smoke.m"))
        val dylib = compileDylib("objcsmoke", mSources)
        if (testRunSettings.configurables.targetTriple.isSimulator)
            codesign(dylib.resultingArtifact.path)
        val stringsdict = interopObjCDir.resolve("Localizable.stringsdict")
        stringsdict.copyTo(buildDir.resolve("en.lproj/Localizable.stringsdict"), overwrite = true)

        val cinteropKlib = cinteropToLibrary(
            targets = targets,
            defFile = interopObjCDir.resolve("objcSmoke.def"),
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs(emptyList(), cinteropArgs = listOf("-header", "smoke.h"))
        ).assertSuccess().resultingArtifact

        val testCase = generateTestCaseWithSingleFile(
            sourceFile = interopObjCDir.resolve("$ktFilePrefix.kt"),
            moduleName = "smoke",
            freeCompilerArgs = TestCompilerArgs(
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                "-linker-option",
                "-L${buildDir.absolutePath}"
            ),
            testKind = TestKind.STANDALONE_NO_TR,
            extras = TestCase.NoTestRunnerExtras("main"),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout).copy(
                outputDataFile = TestRunCheck.OutputDataFile(file = interopObjCDir.resolve("$ktFilePrefix.out"))
            )
        )
        val success = compileToExecutable(testCase, cinteropKlib.asLibraryDependency()).assertSuccess()
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
        Assumptions.assumeTrue(targets.testTarget.family.isAppleFamily)
        val mSources = interopObjCDir.resolve("tests").listFiles { file: File -> file.name.endsWith(".m") }!!.toList()
        val dylib = compileDylib("objctests", mSources)
        if (testRunSettings.configurables.targetTriple.isSimulator)
            codesign(dylib.resultingArtifact.path)

        val hFiles = interopObjCDir.resolve("tests").listFiles { file: File -> file.name.endsWith(".h") }!!.toList()
        val cinteropKlib = cinteropToLibrary(
            targets = targets,
            defFile = interopObjCDir.resolve("objcTests.def"),
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs(emptyList(), cinteropArgs = hFiles.flatMap { listOf("-header", "tests/${it.name}") })
        ).assertSuccess().resultingArtifact

        val ignoredTestGTestPatterns = if (testRunSettings.get<GCType>() != GCType.NOOP) emptySet() else setOf(
            "Kt41811Kt.*",
            "CustomStringKt.testCustomString",
            "Kt42482Kt.testKT42482",
            "ObjcWeakRefsKt.testObjCWeakRef",
            "WeakRefsKt.testWeakRefs",
        )
        val testCase = generateTestCaseWithSingleModule(
            moduleDir = interopObjCDir.resolve("tests"),
            freeCompilerArgs = TestCompilerArgs(
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                "-opt-in=kotlin.native.internal.InternalForKotlinNative",
                "-XXLanguage:+ImplicitSignedToUnsignedIntegerConversion",
                "-tr", "-e", "main", "-linker-option", "-L${buildDir.absolutePath}"
            ),
            extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT, ignoredTestGTestPatterns),
        )
        val success = compileToExecutable(testCase, cinteropKlib.asLibraryDependency()).assertSuccess()
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
        assertEquals("OK", execResult.stdout)
    }

    @Test
    @TestMetadata("messaging")
    fun testMessaging() {
        testDylibCinteropExe("messaging")
    }

    @Test
    @TestMetadata("kt42172")
    fun testKt42172() {
        Assumptions.assumeFalse(testRunSettings.get<GCType>() == GCType.NOOP)
        val execResult = testDylibCinteropExe("kt42172")
        assertEquals("Executed finalizer", execResult.stdout)
    }

    @Test
    @TestMetadata("include_categories")
    fun testInclude_categories() {
        val execResult = testDylibCinteropExe("include_categories")
        assertEquals("""
            3.0
            3.14
            6.0
            
            3
            6
            6.0
            
            3.0
            600.0
        """.trimIndent(), execResult.stdout)
    }

    @Test
    @TestMetadata("kt56402")
    fun testKt56402() {
        // Test depends on macOS-specific AppKit and some GC
        Assumptions.assumeTrue(targets.testTarget.family == Family.OSX)
        Assumptions.assumeFalse(testRunSettings.get<GCType>() == GCType.NOOP)
        val execResult = testDylibCinteropExe(
            "kt56402",
            extraClangOpts = listOf("-framework", "AppKit", "-fobjc-arc"),
            extraCompilerOpts = listOf("-tr"),
        )
        assertTrue(execResult.stdout.contains("[  PASSED  ] 8 tests"))
    }

    @Test
    @TestMetadata("friendly_dealloc")
    fun testFriendly_dealloc() {
        Assumptions.assumeFalse(testRunSettings.get<GCType>() == GCType.NOOP)
        val execResult = testDylibCinteropExe(
            "friendly_dealloc",
            extraClangOpts = listOf("-fno-objc-arc"),
            extraCompilerOpts = listOf("-tr", "-XXLanguage:+ImplicitSignedToUnsignedIntegerConversion"),
            extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT),
        )
        assertTrue(execResult.stdout.contains("[  PASSED  ] 4 tests"))
    }

    @Test
    @TestMetadata("objCAction")
    fun testObjCAction() {
        val execResult = testDylibCinteropExe(
            "objCAction",
            extraCompilerOpts = listOf("-tr"),
            extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT),
        )
        assertTrue(execResult.stdout.contains("[  PASSED  ] 5 tests"))
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
        assertEquals("", res.stdout)
        assertEquals("", res.stderr)
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
        assertEquals("OK: Ends with uncaught exception handler", res.stdout)
        assertEquals("", res.stderr)
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
        assertEquals("OK: ForeignException", res.stdout)
        assertEquals("", res.stderr)
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

        val cinteropKlib = cinteropToLibrary(
            targets = targets,
            defFile = srcDir.resolve(defFile),
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs(
                compilerArgs = emptyList(),
                cinteropArgs = extraCinteropArgs + listOf("-header", srcDir.resolve(hFile).absolutePath)
            )
        ).assertSuccess().resultingArtifact

        val testKind = when (extras) {
            is TestCase.NoTestRunnerExtras -> TestKind.STANDALONE_NO_TR
            is TestCase.WithTestRunnerExtras -> TestKind.STANDALONE
        }
        val testCase = generateTestCaseWithSingleFile(
            sourceFile = srcDir.resolve(ktFile),
            moduleName = testName,
            testKind = testKind,
            extras = extras,
            freeCompilerArgs = TestCompilerArgs(
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                "-opt-in=kotlin.native.internal.InternalForKotlinNative",
                "-linker-option", "-L${buildDir.absolutePath}",
                *extraCompilerOpts.toTypedArray()
            )
        )

        val success = compileToExecutable(testCase, cinteropKlib.asLibraryDependency()).assertSuccess()
        return testRunSettings.executor.runProcess(success.resultingArtifact.executableFile.absolutePath)
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
        val cinteropKlib = cinteropToLibrary(
            targets = targets,
            defFile = withSpacesDef,
            outputDir = buildDir,
            freeCompilerArgs = TestCompilerArgs.EMPTY
        ).assertSuccess().resultingArtifact

        val testCase = generateTestCaseWithSingleFile(
            sourceFile = srcDir.resolve("withSpaces.kt"),
            freeCompilerArgs = TestCompilerArgs(
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
            ),
            testKind = TestKind.STANDALONE_NO_TR,
            extras = TestCase.NoTestRunnerExtras("main"),
        )
        val success = compileToExecutable(testCase, cinteropKlib.asLibraryDependency()).assertSuccess()
        val testExecutable = TestExecutable(
            success.resultingArtifact,
            success.loggedData,
            listOf(TestName("interop_objc_tests"))
        )
        runExecutableAndVerify(testCase, testExecutable)
        assertTrue(mapfile.exists())
    }

    private fun compileDylib(name: String, mSources: List<File>, extraClangOpts: List<String> = listOf("-fobjc-arc")): TestCompilationResult.Success<out TestCompilationArtifact.Executable> {
        val clangResult = compileWithClang(
            clangDistribution = ClangDistribution.Llvm,
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
}