/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCaseId
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClangToStaticLibrary
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContains

private const val TEST_DATA_DIR = "native/native.tests/testData/interop/staticLibraryPaths"

/**
 * Make sure that, when compiling and linking for Linux targets from Windows hosts,
 * the static library paths are properly quoted.
 * For example, when passing them to a linker via a `@file`. See e.g, KT-89637.
 */
@Tag("cinterop")
@TestDataPath("\$PROJECT_ROOT")
@EnforcedProperty(ClassLevelProperty.TEST_TARGET, "linux_x64")
@EnforcedProperty(ClassLevelProperty.COMPILE_ONLY, "true")
class CInteropStaticLibraryWindowsHostPathTest : AbstractNativeSimpleTest() {
    @Test
    @TestMetadata(TEST_DATA_DIR)
    fun testWindowsHostPath() {
        Assumptions.assumeTrue(HostManager.hostIsMingw)
        // All Windows paths contain backslashes (also checked in the called function).
        linkWithStaticLibraryIn(buildDir)
    }
}

/**
 * Similar to the above, but on non-Windows hosts.
 */
@Tag("cinterop")
@TestDataPath("\$PROJECT_ROOT")
class CInteropStaticLibraryPathsTest : AbstractNativeSimpleTest() {
    @Test
    @TestMetadata(TEST_DATA_DIR)
    fun testPathWithBackslashQuoteAndSpace() {
        Assumptions.assumeTrue(targets.testTarget.family == Family.LINUX)
        Assumptions.assumeFalse(HostManager.hostIsMingw, "Backslashes and double quotes are not allowed in Windows file names")
        linkWithStaticLibraryIn(buildDir.resolve("a\\\"b c'd"))
    }
}

/**
 * Builds a static library in [dir], embeds it into an unpacked cinterop klib in [dir], and links an executable with it.
 * The linker thus gets the path of the static library embedded into the klib, which is located in [dir].
 */
private fun AbstractNativeSimpleTest.linkWithStaticLibraryIn(dir: File) {
    val testDataDir = ForTestCompileRuntime.transformTestDataPath(TEST_DATA_DIR)
    dir.mkdirs()

    compileWithClangToStaticLibrary(
        sourceFiles = listOf(testDataDir.resolve("staticLibraryPaths.c")),
        outputFile = dir.resolve("staticLibraryPaths.a"),
    ).assertSuccess()

    val libraryPath = dir.absolutePath
    // Make sure we do test paths with backslashes here:
    assertContains(libraryPath, "\\")

    val cinteropKlib = cinteropToLibrary(
        defFile = testDataDir.resolve("staticLibraryPaths.def"),
        outputDir = dir,
        freeCompilerArgs = TestCompilerArgs(
            compilerArgs = emptyList(),
            cinteropArgs = listOf(
                "-pkg", "staticLibraryPaths",
                "-libraryPath", libraryPath,
                "-nopack", // Keep the embedded static library in [dir] instead of extracting it to a temporary directory.
            )
        )
    ).assertSuccess().resultingArtifact

    val kotlinKlib = compileLibrary(
        testRunSettings,
        source = testDataDir.resolve("main.kt"),
        dependencies = listOf(cinteropKlib),
    ).assertSuccess().resultingArtifact

    val testCase = TestCase(
        id = TestCaseId.Named("staticLibraryPaths"),
        kind = TestKind.STANDALONE_NO_TR,
        modules = emptySet(),
        freeCompilerArgs = TestCompilerArgs.EMPTY,
        nominalPackageName = PackageName.EMPTY,
        checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        extras = TestCase.NoTestRunnerExtras("main"),
    ).apply {
        initialize(null, null)
    }

    val executableResult = ExecutableCompilation(
        testRunSettings,
        freeCompilerArgs = testCase.freeCompilerArgs,
        sourceModules = testCase.modules,
        extras = testCase.extras,
        dependencies = setOf(cinteropKlib.asLibraryDependency(), kotlinKlib.asLibraryDependency()),
        expectedArtifact = TestCompilationArtifact.Executable(
            buildDir.resolve("main." + targets.testTarget.family.exeSuffix)
        ),
    ).result.assertSuccess()

    runExecutableAndVerify(testCase, TestExecutable.fromCompilationResult(testCase, executableResult))
}
