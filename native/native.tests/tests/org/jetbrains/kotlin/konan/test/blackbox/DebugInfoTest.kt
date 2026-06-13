/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.native.executors.runProcess
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for debug info path remapping features:
 * - `-Xdebug-prefix-map=<old>=<new>` - remaps source file paths in DWARF debug info
 * - `-Xbinary=debugCompilationDir=<path>` - sets DW_AT_comp_dir in DWARF compilation unit
 */
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
@EnforcedProperty(ClassLevelProperty.CACHE_MODE, "NO")
class DebugInfoTest : AbstractNativeSimpleTest() {

    /**
     * DWARF on Windows uses backslashes in paths. Make sure the prefix map uses
     * the same path format as the debug info.
     */
    private fun debugInfoOriginalPrefix(rootDir: File): String =
        if (targets.testTarget.family == Family.MINGW) rootDir.absolutePath
        else rootDir.absolutePath.replace("\\", "/")

    /**
     * Check both slash styles when asserting "original path not present".
     */
    private fun allOriginalPrefixes(rootDir: File): List<String> =
        listOf(rootDir.absolutePath, rootDir.absolutePath.replace("\\", "/")).distinct()

    /**
     * Skip test on targets that don't produce DWARF debug info
     */
    private fun assumeDwarfSupported() {
        val supportedFamilies = setOf(Family.OSX, Family.LINUX, Family.IOS, Family.TVOS, Family.WATCHOS, Family.MINGW)
        Assumptions.assumeTrue(
            targets.testTarget.family in supportedFamilies,
            "DWARF debug info test only applicable to targets with DWARF support"
        )
    }

    /**
     * Get the path to llvm-dwarfdump bundled with Kotlin/Native.
     */
    private fun getLlvmDwarfDumpPath(): String {
        val configurables = testRunSettings.configurables
        val toolName = if (targets.hostTarget.family == Family.MINGW) "llvm-dwarfdump.exe" else "llvm-dwarfdump"
        return "${configurables.absoluteLlvmHome}/bin/$toolName"
    }

    /**
     * Find the actual executable file, considering platform-specific extensions.
     * On Windows (MinGW), executables have .exe extension added by the compiler.
     */
    private fun findActualExecutable(executable: File): File {
        if (executable.exists()) return executable
        // On Windows, the compiler adds .exe extension
        val withExe = File(executable.absolutePath + ".exe")
        if (withExe.exists()) return withExe
        // On other platforms, try .kexe extension
        val withKexe = File(executable.absolutePath + ".kexe")
        if (withKexe.exists()) return withKexe
        // Return original if nothing found (will fail with clear error)
        return executable
    }

    /**
     * Run llvm-dwarfdump on the executable and return the output.
     */
    private fun runDwarfDump(executable: File): String {
        val actualExecutable = findActualExecutable(executable)
        val dwarfDump = getLlvmDwarfDumpPath()
        val result = runProcess(dwarfDump, "--debug-info", actualExecutable.absolutePath) {
            timeout = 1.minutes
        }
        return result.stdout
    }

    /**
     * Test that -Xdebug-prefix-map correctly remaps source file paths in DWARF debug info.
     */
    @Test
    fun testDebugPrefixMap() {
        assumeDwarfSupported()

        val rootDir = File("native/native.tests/testData/debugInfo")
        val sourceFile = rootDir.resolve("debugPrefixMap.kt")

        // Use the absolute path of the testData directory as the "old" prefix to remap
        val originalPath = debugInfoOriginalPrefix(rootDir)
        val remappedPath = "/remapped/path"

        val testCase = generateTestCaseWithSingleFile(
            sourceFile,
            freeCompilerArgs = TestCompilerArgs(
                "-g",
                "-Xdebug-prefix-map=$originalPath=$remappedPath"
            ),
            extras = TestCase.NoTestRunnerExtras("main"),
            testKind = TestKind.STANDALONE_NO_TR,
        )

        val expectedArtifact = TestCompilationArtifact.Executable(buildDir.resolve("debug_prefix_map_test"))
        val compilation = ExecutableCompilation(
            testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = emptyList(),
            expectedArtifact = expectedArtifact,
        )

        val compilationResult = compilation.result.assertSuccess()
        val executable = compilationResult.resultingArtifact.executableFile

        val dwarfOutput = runDwarfDump(executable)

        // Verify that the remapped path appears in DWARF output
        assertContains(
            dwarfOutput,
            remappedPath,
            message = "DWARF output should contain the remapped path '$remappedPath'"
        )

        // Verify that the original path does NOT appear in DWARF output
        // The remapped path should be used for directory, so the full original path should not be present
        assertFalse(
            allOriginalPrefixes(rootDir).any { dwarfOutput.contains(it) },
            "DWARF output should not contain the original path '$originalPath' after remapping"
        )
    }

    /**
     * Test that -Xbinary=debugCompilationDir correctly sets DW_AT_comp_dir in DWARF.
     */
    @Test
    fun testDebugCompilationDir() {
        assumeDwarfSupported()

        val rootDir = File("native/native.tests/testData/debugInfo")
        val sourceFile = rootDir.resolve("debugCompilationDir.kt")

        val customCompDir = "/custom/compilation/dir"

        val testCase = generateTestCaseWithSingleFile(
            sourceFile,
            freeCompilerArgs = TestCompilerArgs(
                "-g",
                "-Xbinary=debugCompilationDir=$customCompDir"
            ),
            extras = TestCase.NoTestRunnerExtras("main"),
            testKind = TestKind.STANDALONE_NO_TR,
        )

        val expectedArtifact = TestCompilationArtifact.Executable(buildDir.resolve("debug_compilation_dir_test"))
        val compilation = ExecutableCompilation(
            testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = emptyList(),
            expectedArtifact = expectedArtifact,
        )

        val compilationResult = compilation.result.assertSuccess()
        val executable = compilationResult.resultingArtifact.executableFile

        val dwarfOutput = runDwarfDump(executable)

        // Verify that DW_AT_comp_dir contains the custom compilation directory
        assertContains(
            dwarfOutput,
            "DW_AT_comp_dir",
            message = "DWARF output should contain DW_AT_comp_dir attribute"
        )
        assertContains(
            dwarfOutput,
            customCompDir,
            message = "DWARF output should contain the custom compilation directory '$customCompDir'"
        )
    }

    /**
     * Test that multiple -Xdebug-prefix-map arguments work together.
     */
    @Test
    fun testMultipleDebugPrefixMaps() {
        assumeDwarfSupported()

        val rootDir = File("native/native.tests/testData/debugInfo")
        val sourceFile = rootDir.resolve("debugPrefixMap.kt")

        val originalPath1 = debugInfoOriginalPrefix(rootDir)
        val remappedPath1 = "/first/remap"

        // Also test a second mapping that won't match (but should not cause errors)
        val originalPath2 = "/nonexistent/path"
        val remappedPath2 = "/second/remap"

        val testCase = generateTestCaseWithSingleFile(
            sourceFile,
            freeCompilerArgs = TestCompilerArgs(
                "-g",
                "-Xdebug-prefix-map=$originalPath1=$remappedPath1",
                "-Xdebug-prefix-map=$originalPath2=$remappedPath2"
            ),
            extras = TestCase.NoTestRunnerExtras("main"),
            testKind = TestKind.STANDALONE_NO_TR,
        )

        val expectedArtifact = TestCompilationArtifact.Executable(buildDir.resolve("debug_multi_prefix_map_test"))
        val compilation = ExecutableCompilation(
            testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = emptyList(),
            expectedArtifact = expectedArtifact,
        )

        val compilationResult = compilation.result.assertSuccess()
        val executable = compilationResult.resultingArtifact.executableFile

        val dwarfOutput = runDwarfDump(executable)

        // Verify that the first remapped path appears
        assertContains(
            dwarfOutput,
            remappedPath1,
            message = "DWARF output should contain the first remapped path '$remappedPath1'"
        )
    }

    /**
     * Test that both -Xdebug-prefix-map and -Xbinary=debugCompilationDir can be used together.
     */
    @Test
    fun testCombinedDebugOptions() {
        assumeDwarfSupported()

        val rootDir = File("native/native.tests/testData/debugInfo")
        val sourceFile = rootDir.resolve("debugPrefixMap.kt")

        val originalPath = debugInfoOriginalPrefix(rootDir)
        val remappedPath = "/combined/remapped"
        val customCompDir = "/combined/comp/dir"

        val testCase = generateTestCaseWithSingleFile(
            sourceFile,
            freeCompilerArgs = TestCompilerArgs(
                "-g",
                "-Xdebug-prefix-map=$originalPath=$remappedPath",
                "-Xbinary=debugCompilationDir=$customCompDir"
            ),
            extras = TestCase.NoTestRunnerExtras("main"),
            testKind = TestKind.STANDALONE_NO_TR,
        )

        val expectedArtifact = TestCompilationArtifact.Executable(buildDir.resolve("debug_combined_test"))
        val compilation = ExecutableCompilation(
            testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = emptyList(),
            expectedArtifact = expectedArtifact,
        )

        val compilationResult = compilation.result.assertSuccess()
        val executable = compilationResult.resultingArtifact.executableFile

        val dwarfOutput = runDwarfDump(executable)

        // Both should be applied
        assertContains(
            dwarfOutput,
            remappedPath,
            message = "DWARF output should contain the remapped path '$remappedPath'"
        )
        assertContains(
            dwarfOutput,
            customCompDir,
            message = "DWARF output should contain the custom compilation directory '$customCompDir'"
        )
    }
}
