/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.generateTestCaseWithSingleFile
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SearchPathResolver
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertContainsElements
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertNotEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import java.io.File
import kotlin.text.contains

/**
 * This test class needs to set up a custom working directory in the JVM process. This is necessary to trigger
 * the special behavior inside the [SearchPathResolver] to start looking for KLIBs by relative path (or just
 * by library name aka `unique_name`) inside the working directory.
 *
 * To make this possible and to avoid side effects for other tests two special annotations
 * are added: `@Isolated` and `@Execution(ExecutionMode.SAME_THREAD)`.
 *
 * The control over the working directory in performed in the [runWithCustomWorkingDir] function.
 */
@Tag("klib")
@Isolated // Run this test class in isolation from other test classes.
@Execution(ExecutionMode.SAME_THREAD) // Run all test functions sequentially in the same thread.
class KlibCliLibraryPathVariationsTest : AbstractNativeSimpleTest() {
    @Test
    @DisplayName("stdlib and posix passed to the compiler in different ways")
    fun testStdlibAndPosixPassedToTheCompilerInDifferentWays() = with(NonRepeatedModuleNameGenerator()) {
        val librariesDir = testRunSettings.get<KotlinNativeHome>().librariesDir
        val target = testRunSettings.get<KotlinNativeTargets>().testTarget

        val stdlibFile = librariesDir.resolve("common/stdlib")
        val posixFile = librariesDir.resolve("platform/${target.name}/org.jetbrains.kotlin.native.platform.posix")

        sequenceOf(stdlibFile, posixFile)
            .flatMap {
                sequenceOf(
                    it.absolutePath,
                    it.resolve("../${it.name}").absolutePath,
                    it.resolve("default/..").absolutePath,

                    // Note: stdlib & platform libraries (such as posix, for example) have identical
                    // unique_name and name of the directory where the unpacked library is located.
                    // So, practically it's not possible to check if the "resolver" resolves the library
                    // strictly by its unique_name or strictly by the last segment of its path (i.e.,
                    // by the relative path) by just observing the results of its work.
                    // Nevertheless, for libraries in the Native distribution it is OK if they are resolved
                    // by "name" whichever this name would be.
                    it.name
                )
            }
            .forEach { libraryNameOrPath ->
                compileMainModule(libraryNameOrPath)
                compileMainModule(libraryNameOrPath, extraCliArgs = listOf("-nostdlib", "-no-default-libs"))
            }

        sequenceOf(stdlibFile, posixFile)
            .map { it.name }
            .flatMap { sequenceOf("./$it", "../build/$it", "$it/../$it") }
            .flatMap { sequenceOf(it, "$it.klib") }
            .forEach { libraryNameOrPath ->
                expectFailingAsNotFound(libraryNameOrPath) { compileMainModule(libraryNameOrPath) }
            }
    }

    @Test
    @DisplayName("Custom library with custom unique name passed to the compiler by the absolute or the relative path")
    fun testCustomLibraryWithCustomUniqueNamePassedToTheCompilerByTheAbsoluteOrTheRelativePath() {
        with(NonRepeatedModuleNameGenerator(customUniqueName = true)) {
            customLibraryWithCustomUniqueNamePassedToTheCompilerByTheAbsoluteOrTheRelativePath(true)
            customLibraryWithCustomUniqueNamePassedToTheCompilerByTheAbsoluteOrTheRelativePath(false)
        }
    }

    private fun NonRepeatedModuleNameGenerator.customLibraryWithCustomUniqueNamePassedToTheCompilerByTheAbsoluteOrTheRelativePath(
        produceUnpackedKlib: Boolean,
    ) {
        val dependency1File = compileDependencyModule(produceUnpackedKlib)
        val dependency1FileName = dependency1File.name // This is just a name of a KLIB file or a KLIB directory, not the unique name!
        val dependency1UniqueName = dependency1File.readLibrary().uniqueName // And this is the unique name.
        assertNotEquals(dependency1UniqueName, dependency1FileName)

        sequenceOf(
            dependency1File.absolutePath,
            runIf(produceUnpackedKlib) { dependency1File.resolve("../${dependency1File.name}").absolutePath },
            runIf(produceUnpackedKlib) { dependency1File.resolve("default/..").absolutePath },
            dependency1FileName,
            runIf(produceUnpackedKlib) { "$dependency1FileName/../$dependency1FileName" },
            runIf(produceUnpackedKlib) { "$dependency1FileName/default/.." },
        ).filterNotNull().forEach { dependency1Path ->
            // Passing the dependency by absolute or relative path should be OK:
            val mainLibrary = compileMainModule(dependency1Path)
            val mainLibraryDependencies = mainLibrary.readLibrary().dependencies
            assertContainsElements(mainLibraryDependencies, dependency1UniqueName)
        }

        // Passing the dependency by the unique name should fail. The KLIB resolver is able to locate KLIBs only by paths:
        expectFailingAsNotFound(dependency1UniqueName) { compileMainModule(dependency1UniqueName) }

        val dependency2File = compileDependencyModule(produceUnpackedKlib, listOf("-l", dependency1File.absolutePath))
        val dependency2FileName = dependency2File.name // This is just a name of a KLIB file or a KLIB directory, not the unique name!
        val dependency2UniqueName = dependency2File.readLibrary().uniqueName // And this is the unique name.
        assertNotEquals(dependency2UniqueName, dependency2FileName)

        val dependency2Dependencies = dependency2File.readLibrary().dependencies
        assertContainsElements(dependency2Dependencies, dependency1UniqueName)

        // Passing both direct and transitive dependencies by path should be OK:
        run {
            val mainLibraryFile = compileMainModule(
                dependency1File.absolutePath,
                extraCliArgs = listOf("-l", dependency2File.absolutePath)
            )
            val mainLibraryDependencies = mainLibraryFile.readLibrary().dependencies
            assertContainsElements(mainLibraryDependencies, dependency1UniqueName)
            assertContainsElements(mainLibraryDependencies, dependency2UniqueName)
        }

        // Passing the direct dependency by path and not passing transitive should be OK with warning.
        // Transitive dependency won't be recorded in the main library `depends=` property.
        run {
            val mainLibraryFile = compileMainModule(
                dependency2File.absolutePath,
                warningHandler = { warning ->
                    assertTrue(warning.startsWith("warning: KLIB resolver: Could not find \"$dependency1UniqueName\""))
                })
            val mainLibraryDependencies = mainLibraryFile.readLibrary().dependencies
            assertFalse(dependency1UniqueName in mainLibraryDependencies)
            assertContainsElements(mainLibraryDependencies, dependency2UniqueName)
        }

        // Passing the direct dependency by path and passing the transitive one by unique name should fail:
        expectFailingAsNotFound(dependency1UniqueName) {
            compileMainModule(
                dependency2File.absolutePath,
                extraCliArgs = listOf("-l", dependency1UniqueName)
            )
        }

        // Passing both dependencies by path and additionally the transitive one by unique name:
        expectFailingDueToPassedUniqueName(dependency1File.absolutePath, dependency1UniqueName) {
            compileMainModule(
                dependency2File.absolutePath,
                extraCliArgs = listOf("-l", dependency1File.absolutePath, "-l", dependency1UniqueName)
            )
        }
    }

    private fun NonRepeatedModuleNameGenerator.compileDependencyModule(
        produceUnpackedKlib: Boolean,
        extraCliArgs: List<String> = emptyList(),
    ): File {
        return compileSingleModule(
            moduleBaseName = "lib",
            produceUnpackedKlib = produceUnpackedKlib,
            warningHandler = null,
            cliArgs = extraCliArgs
        )
    }

    private fun NonRepeatedModuleNameGenerator.compileMainModule(
        dependency: String,
        warningHandler: ((String) -> Unit)? = null,
        extraCliArgs: List<String> = emptyList(),
    ): File {
        return compileSingleModule(
            moduleBaseName = "main",
            produceUnpackedKlib = true, // does not matter, we test only dependencies of 'main'
            warningHandler,
            cliArgs = listOf("-l", dependency) + extraCliArgs
        )
    }

    private inline fun expectFailingAsNotFound(dependencyNameInError: String, block: () -> Unit) {
        val normalizedDependencyNameInError = dependencyNameInError.replace('/', File.separatorChar)

        try {
            block()
            fail { "The test was expected to fail with the error due to unresolved KLIB \"$dependencyNameInError\". But was successful." }
        } catch (cte: CompilationToolException) {
            // The message can use both forward and backward slashes on Windows.
            assertTrue(
                cte.reason.contains("error: KLIB resolver: Could not find \"$normalizedDependencyNameInError\"") ||
                        cte.reason.contains("error: KLIB resolver: Could not find \"$dependencyNameInError\"")
            ) {
                "The test was expected to fail with the error due to unresolved KLIB \"$dependencyNameInError\". " +
                        "But the actual error message is (${cte.reason.toByteArray().size} bytes, ${cte.reason.lineSequence().count()} lines):\n${cte.reason}"
            }
        }
    }

    private inline fun expectFailingDueToPassedUniqueName(libraryPath: String, uniqueName: String, block: () -> Unit) {
        try {
            block()
            fail("Normally should not get here")
        } catch (cte: CompilationToolException) {
            assertTrue(cte.reason.contains("error: KLIB resolver: Library '$libraryPath' was found by its unique name '$uniqueName'"))
        }
    }

    private fun NonRepeatedModuleNameGenerator.compileSingleModule(
        moduleBaseName: String,
        produceUnpackedKlib: Boolean,
        warningHandler: ((String) -> Unit)?,
        cliArgs: List<String>,
    ): File {
        val moduleNames = generateModuleNames(moduleBaseName)

        val module = newSourceModules { addRegularModule(moduleNames.uniqueName) }.modules.single()

        val klibsDir = buildDir.resolve("klibs").apply(File::mkdirs)
        val libraryFile = klibsDir.resolve(moduleNames.fileName + if (produceUnpackedKlib) "" else ".klib")

        runWithCustomWorkingDir(klibsDir) {
            val testCase = generateTestCaseWithSingleFile(
                sourceFile = module.sourceFile,
                moduleName = module.name,
                TestCompilerArgs(
                    listOfNotNull(
                        "-module-name", moduleNames.uniqueName,
                        "-nopack".takeIf { produceUnpackedKlib },
                    ) + cliArgs
                )
            )

            val compilation = LibraryCompilation(
                settings = testRunSettings,
                freeCompilerArgs = testCase.freeCompilerArgs,
                sourceModules = testCase.modules,
                dependencies = emptySet(),
                expectedArtifact = KLIB(libraryFile)
            )

            val compilerCall = compilation.result.assertSuccess().loggedData as LoggedData.CompilationToolCall
            assertEquals(ExitCode.OK, compilerCall.exitCode)

            val warnings = compilerCall.toolOutput.lineSequence().filter { "warning: KLIB resolver:" in it }.toList()
            if (warnings.isNotEmpty()) {
                if (warningHandler != null) {
                    warnings.forEach(warningHandler)
                } else {
                    fail { compilerCall.toolOutput }
                }
            }

            val actualUniqueName = libraryFile.readLibrary().uniqueName
            assertEquals(moduleNames.uniqueName, actualUniqueName)
        }

        return libraryFile
    }

    private fun File.readLibrary(): KotlinLibrary = KlibLoader { libraryPaths(this@readLibrary) }.load().librariesStdlibFirst.single()

    private val KotlinLibrary.dependencies: Set<String>
        get() = manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).toSet()

    /**
     * Helps to avoid generating modules with same names and paths when [compileToKlibsViaCli] is called repetitively
     * within a single test method execution.
     */
    private class NonRepeatedModuleNameGenerator(private val customUniqueName: Boolean = false) {
        data class ModuleNames(val uniqueName: String, val fileName: String)

        private var counter = 0

        fun generateModuleNames(baseName: String): ModuleNames {
            val fileName = baseName + (counter++).toString().padStart(2, '0')
            val uniqueName = if (customUniqueName) "unique_$fileName" else fileName
            return ModuleNames(uniqueName, fileName)
        }
    }

    companion object {
        private const val USER_DIR = "user.dir"

        private inline fun runWithCustomWorkingDir(customWorkingDir: File, block: () -> Unit) {
            val previousWorkingDir: String = System.getProperty(USER_DIR)
            try {
                System.setProperty(USER_DIR, customWorkingDir.absolutePath)
                block()
            } finally {
                System.setProperty(USER_DIR, previousWorkingDir)
            }
        }
    }

}