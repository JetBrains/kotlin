/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.test.blackbox.*
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertContainsElements
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertNotEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail
import org.jetbrains.kotlin.test.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.junit.jupiter.api.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KFile

/**
 * This test class needs to set up a custom working directory in the JVM process. This is necessary to trigger
 * the special behavior inside the [SearchPathResolver] to start looking for KLIBs by relative path (or just
 * by library name aka `unique_name`) inside the working directory.
 *
 * In order to make this possible and in order to avoid side effects for other tests two special annotations
 * are added: `@Isolated` and `@Execution(ExecutionMode.SAME_THREAD)`.
 *
 * The control over the working directory in performed in the [runWithCustomWorkingDir] function.
 */
@Tag("klib")
@Isolated // Run this test class in isolation from other test classes.
@Execution(ExecutionMode.SAME_THREAD) // Run all test functions sequentially in the same thread.
class KlibResolverTest : AbstractNativeSimpleTest() {
    private data class Module(val name: String, val dependencyNames: List<String>) {
        constructor(name: String, vararg dependencyNames: String) : this(name, dependencyNames.asList())

        lateinit var dependencies: List<Module>
        lateinit var sourceFile: File

        fun initDependencies(resolveDependency: (String) -> Module) {
            dependencies = dependencyNames.map(resolveDependency)
        }
    }

    @Test
    @DisplayName("Test resolving all dependencies recorded in `depends` / `dependency_version` properties (KT-63931)")
    fun testResolvingDependenciesRecordedInManifest() {
        val modules = createModules(
            Module("a"),
            Module("b", "a"),
            Module("c", "a"),
            Module("d", "b", "c", "a"),
        )

        listOf(
            false to false,
            true to false,
            true to true,
            false to true,
        ).forEach { (produceUnpackedKlibs, useLibraryNamesInCliArguments) ->
            modules.compileModules(produceUnpackedKlibs, useLibraryNamesInCliArguments)
        }
    }

    @Test
    @DisplayName("Test resolving nonexistent transitive dependency recorded in `depends` property (KT-70146)")
    fun testResolvingTransitiveDependenciesRecordedInManifest() {
        val moduleA = Module("a")
        val moduleB = Module("b", "a")
        val moduleC = Module("c", "b")
        val modules = createModules(moduleA, moduleB, moduleC)

        var aKlib: KLIB? = null
        modules.compileModules(produceUnpackedKlibs = false, useLibraryNamesInCliArguments = true) { module, successKlib ->
            when (module.name) {
                "a" -> aKlib = successKlib.resultingArtifact
                "b" -> aKlib!!.klibFile.delete() // remove transitive dependency `a`, so subsequent compilation of `c` would miss it.
                "c" -> {
                    val compilationToolCall = successKlib.loggedData as LoggedData.CompilationToolCall
                    assertEquals(ExitCode.OK, compilationToolCall.exitCode)
                    assertTrue(compilationToolCall.toolOutput.contains("warning: KLIB resolver: Could not find \"a\""))
                }
            }
        }
    }

    @Test
    @DisplayName("-nowarn cmdline param suppresses warning when resolving nonexistent transitive dependency recorded in `depends` property (KT-70146)")
    fun testResolvingTransitiveDependenciesRecordedInManifestWithNowarn() {
        val moduleA = Module("a")
        val moduleB = Module("b", "a")
        val moduleC = Module("c", "b")
        val modules = createModules(moduleA, moduleB, moduleC)

        var aKlib: KLIB? = null
        modules.compileModules(produceUnpackedKlibs = false, useLibraryNamesInCliArguments = true, extraCmdLineParams = listOf("-nowarn")) { module, successKlib ->
            when (module.name) {
                "a" -> aKlib = successKlib.resultingArtifact
                "b" -> aKlib!!.klibFile.delete() // remove transitive dependency `a`, so subsequent compilation of `c` would miss it.
                "c" -> {
                    val compilationToolCall = successKlib.loggedData as LoggedData.CompilationToolCall
                    assertEquals(ExitCode.OK, compilationToolCall.exitCode)
                    assertFalse(compilationToolCall.toolOutput.contains("warning: KLIB resolver: Could not find \"a\""))
                }
            }
        }
    }

    @Test
    @DisplayName("-Werror cmdline param causes error resolving nonexistent transitive dependency recorded in `depends` property (KT-70146)")
    fun testResolvingTransitiveDependenciesRecordedInManifestWithWerror() {
        val moduleA = Module("a")
        val moduleB = Module("b", "a")
        val moduleC = Module("c", "b")
        val modules = createModules(moduleA, moduleB, moduleC)

        var aKlib: KLIB? = null
        try {
            modules.compileModules(
                produceUnpackedKlibs = false,
                useLibraryNamesInCliArguments = true,
                extraCmdLineParams = listOf("-Werror"),
            ) { module, successKlib ->
                when (module.name) {
                    "a" -> aKlib = successKlib.resultingArtifact
                    "b" -> aKlib!!.klibFile.delete() // remove transitive dependency `a`, so subsequent compilation of `c` would miss it.
                    "c" -> fail ("Normally should not get here")
                }
            }
        } catch (cte: CompilationToolException) {
            assertTrue(cte.reason.contains("warning: KLIB resolver: Could not find \"a\" in "))
            assertTrue(cte.reason.contains("error: warnings found and -Werror specified"))
        }
    }

    @Test
    @DisplayName("-Werror and -nowarn cmdline params cause error resolving nonexistent transitive dependency recorded in `depends` property (KT-70146)")
    fun testResolvingTransitiveDependenciesRecordedInManifestWithWerrorNowarn() {
        val moduleA = Module("a")
        val moduleB = Module("b", "a")
        val moduleC = Module("c", "b")
        val modules = createModules(moduleA, moduleB, moduleC)

        var aKlib: KLIB? = null
        try {
            modules.compileModules(
                produceUnpackedKlibs = false,
                useLibraryNamesInCliArguments = true,
                extraCmdLineParams = listOf("-Werror", "-nowarn"),
            ) { module, successKlib ->
                when (module.name) {
                    "a" -> aKlib = successKlib.resultingArtifact
                    "b" -> aKlib!!.klibFile.delete() // remove transitive dependency `a`, so subsequent compilation of `c` would miss it.
                    "c" -> fail ("Normally should not get here")
                }
            }
        } catch (cte: CompilationToolException) {
            assertTrue(cte.reason.contains("warning: KLIB resolver: Could not find \"a\" in "))
            assertTrue(cte.reason.contains("error: warnings found and -Werror specified"))
        }
    }

    @Test
    fun testWarningAboutRejectedLibraryIsNotSuppressed() {
        val modules = createModules(
            Module("lib1"),
            Module("lib2", "lib1"),
        )

        // Control compilation -- should finish successfully.
        modules.compileModules(
            produceUnpackedKlibs = true,
            useLibraryNamesInCliArguments = false
        )

        // Compilation with patched manifest -- should fail.
        try {
            modules.compileModules(
                produceUnpackedKlibs = true,
                useLibraryNamesInCliArguments = false
            ) { module, successKlib ->
                if (module.name == "lib1") {
                    patchManifestToBumpAbiVersion(JUnit5Assertions, successKlib.resultingArtifact.klibFile)
                }
            }

            fail { "Normally unreachable code" }
        } catch (cte: CompilationToolException) {
            assertCompilerOutputHasKlibResolverIncompatibleAbiMessages(
                assertions = JUnit5Assertions,
                compilerOutput = cte.reason,
                missingLibrary = "/klib-files.unpacked.paths.transformed/lib1",
                baseDir = buildDir
            )
        }
    }

    @Test
    fun testErrorAboutDuplicatedUniqueNamesInMetadataCompilation() {
        testDuplicatedKlibDependency(
            strategy = DuplicatedUniqueNameStrategy.DENY,
            expectedMessagePrefix = "error",
            freeCompilerArgs = listOf("-Xmetadata-klib")
        )
    }

    @Test
    fun testWarningAboutDuplicatedUniqueNamesInMetadataCompilation() {
        testDuplicatedKlibDependency(
            strategy = DuplicatedUniqueNameStrategy.ALLOW_FIRST_WITH_WARNING,
            expectedMessagePrefix = "warning",
            freeCompilerArgs = listOf("-Xmetadata-klib")
        )
    }

    @Test
    fun testErrorAboutDuplicatedUniqueNamesWithoutCLIParam() {
        testDuplicatedKlibDependency(strategy = null, expectedMessagePrefix = "error")
    }

    @Test
    fun testErrorAboutDuplicatedUniqueNames() {
        testDuplicatedKlibDependency(strategy = DuplicatedUniqueNameStrategy.DENY, expectedMessagePrefix = "error")
    }

    @Test
    fun testWarningAboutDuplicatedUniqueNames() {
        testDuplicatedKlibDependency(strategy = DuplicatedUniqueNameStrategy.ALLOW_FIRST_WITH_WARNING, expectedMessagePrefix = "warning")
    }

    private fun testDuplicatedKlibDependency(
        strategy: DuplicatedUniqueNameStrategy?,
        expectedMessagePrefix: String,
        freeCompilerArgs: List<String>? = null,
    ) {
        val modules = createModules(
            Module("a"),
            Module("b"),
            Module("c", "a", "b"),
        )

        try {
            modules.compileModules(
                produceUnpackedKlibs = true,
                useLibraryNamesInCliArguments = false,
                extraCmdLineParams = buildList {
                    freeCompilerArgs?.let(::addAll)
                    strategy?.asCliArgument()?.let(::add)
                }
            ) { module, successKlib ->
                if (module.name == "a" || module.name == "b") {
                    // set the same `unique_name`
                    patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                        properties[KLIB_PROPERTY_UNIQUE_NAME] = DUPLICATED_UNIQUE_NAME
                    }
                }
            }

            fail { "Normally unreachable code" }
        } catch (cte: CompilationToolException) {
            val compilerOutputLines = cte.reason.lines()
            assertTrue(compilerOutputLines.any {
                it.startsWith("$expectedMessagePrefix: KLIB resolver: The same 'unique_name=$DUPLICATED_UNIQUE_NAME' found in more than one library")
            })
            assertTrue(strategy != DuplicatedUniqueNameStrategy.ALLOW_FIRST_WITH_WARNING || compilerOutputLines.any {
                it.contains("error: unresolved reference")
            })
        }
    }

    @Test
    fun testAllKlibsUsedDespiteWarningAboutDuplicatedUniqueNames() {
        testAllKlibsUsed(listOf(DuplicatedUniqueNameStrategy.ALLOW_ALL_WITH_WARNING.asCliArgument()))
    }

    @Test
    fun testAllKlibsUsedDespiteWarningAboutDuplicatedUniqueNamesInMetadataCompilation() {
        testAllKlibsUsed(
            listOf(
                DuplicatedUniqueNameStrategy.ALLOW_ALL_WITH_WARNING.asCliArgument(),
                "-Xmetadata-klib"
            )
        )
    }

    @Test
    fun testAllKlibsUsedDespiteWarningAboutDuplicatedUniqueNamesInMetadataCompilationWithoutCLIParam() {
        testAllKlibsUsed(listOf("-Xmetadata-klib"))
    }

    private fun testAllKlibsUsed(extraCmdLineParams: List<String>) {
        val modules = createModules(
            Module("a"),
            Module("b"),
            Module("c", "a", "b"),
        )

        modules.compileModules(
            produceUnpackedKlibs = true,
            useLibraryNamesInCliArguments = false,
            extraCmdLineParams = extraCmdLineParams
        ) { module, successKlib ->
            when (module.name) {
                "a", "b" -> patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                    properties[KLIB_PROPERTY_UNIQUE_NAME] = DUPLICATED_UNIQUE_NAME
                }
                "c" -> assertTrue((successKlib.loggedData as LoggedData.CompilationToolCall).toolOutput.lines().any {
                    it.startsWith("warning: KLIB resolver: The same 'unique_name=$DUPLICATED_UNIQUE_NAME' found in more than one library")
                })
            }
        }
    }

    @Test
    fun testIrProvidersMatch() {
        testIrProvidersMismatchImpl(irProvidersMismatchSrcDir, TestCompilerArgs.EMPTY)
    }

    @Test
    fun testIrProvidersMismatch() {
        val freeCompilerArgs = TestCompilerArgs(
            listOf(
                "-manifest",
                irProvidersMismatchSrcDir.resolve("manifest.properties").absolutePath
            )
        )
        try {
            testIrProvidersMismatchImpl(irProvidersMismatchSrcDir, freeCompilerArgs)
            fail { "Normally unreachable code" }
        } catch (cte: CompilationToolException) {
            if (!cte.reason.contains("The library requires unknown IR provider: UNSUPPORTED"))
                throw cte
        }
    }

    @Test
    fun testDependencyVersionsAreNotEnforced() {
        val modules = createModules(
            Module("liba"),
            Module("libb", "liba"),
            Module("libc", "liba", "libb"),
        )

        // Control compilation -- should finish successfully.
        modules.compileModules(
            produceUnpackedKlibs = true,
            useLibraryNamesInCliArguments = false
        )

        // Compilation with patched manifest -- should finish successfully too.
        modules.compileModules(
            produceUnpackedKlibs = true,
            useLibraryNamesInCliArguments = false,
        ) { module, successKlib ->
            when (module.name) {
                "liba" -> {
                    // set the library version = 1.0
                    patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                        @Suppress("DEPRECATION")
                        properties[KLIB_PROPERTY_LIBRARY_VERSION] = "1.0"
                    }
                }
                "libb" -> {
                    // pretend it depends on liba v2.0
                    patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                        // first, check:
                        val dependencyVersionPropertyNames: Set<String> =
                            properties.keys.filter { @Suppress("DEPRECATION") it.startsWith(KLIB_PROPERTY_DEPENDENCY_VERSION) }.toSet()

                        assertTrue(dependencyVersionPropertyNames.isEmpty()) {
                            "Unexpected properties in manifest: ${dependencyVersionPropertyNames.joinToString()}"
                        }

                        // then, patch:
                        @Suppress("DEPRECATION")
                        properties[KLIB_PROPERTY_DEPENDENCY_VERSION + "_liba"] = "2.0"
                    }
                }
            }
        }
    }

    @Test
    fun testDependencyVersionsAreNotAdded() {
        val modules = createModules(
            Module("liba"),
            Module("libb", "liba"),
        )

        // Control compilation -- should finish successfully.
        modules.compileModules(
            produceUnpackedKlibs = true,
            useLibraryNamesInCliArguments = false
        )

        // Compilation with patched manifest -- should finish successfully too.
        modules.compileModules(
            produceUnpackedKlibs = true,
            useLibraryNamesInCliArguments = false,
        ) { module, successKlib ->
            when (module.name) {
                "liba" -> {
                    // set the library version = 1.0
                    patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                        @Suppress("DEPRECATION")
                        properties[KLIB_PROPERTY_LIBRARY_VERSION] = "1.0"
                    }
                }
                "libb" -> {
                    // check that dependency version is set
                    patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                        val dependencyVersionPropertyNames: Set<String> =
                            properties.keys.filter { @Suppress("DEPRECATION") it.startsWith(KLIB_PROPERTY_DEPENDENCY_VERSION) }.toSet()

                        assertTrue(dependencyVersionPropertyNames.isEmpty()) {
                            "Unexpected properties in manifest: ${dependencyVersionPropertyNames.joinToString()}"
                        }
                    }
                }
            }
        }
    }

    @Suppress("BooleanLiteralArgument")
    @Nested
    inner class ResolveViaCliArguments {
        @Test
        @DisplayName("Non-existing library passed to the compiler in different ways")
        fun testNonExistingLibraryPassedToTheCompilerInDifferentWays() = with(NonRepeatedModuleNameGenerator()) {
            sequenceOf("no-such-library")
                .flatMap { sequenceOf(it, "no-such-directory/$it") }
                .flatMap { sequenceOf(it, "./$it", "../$it", "$it/../$it") }
                .flatMap { sequenceOf(it, buildDir.resolve(it).absolutePath) }
                .flatMap { sequenceOf(it, "$it.klib") }
                .forEach { libraryNameOrPath ->
                    expectFailingAsNotFound(libraryNameOrPath) { compileMainModule(libraryNameOrPath) }
                }
        }

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
                .flatMap { sequenceOf("./$it", "../$it", "$it/../$it") }
                .flatMap { sequenceOf(it, "$it.klib") }
                .forEach { libraryNameOrPath ->
                    expectFailingAsNotFound(libraryNameOrPath) { compileMainModule(libraryNameOrPath) }
                }
        }

        @Test
        @DisplayName("Custom library passed to the compiler by the absolute path")
        fun testCustomLibraryPassedToTheCompilerByTheAbsolutePath() = with(NonRepeatedModuleNameGenerator()) {
            customLibraryPassedToTheCompilerByTheAbsolutePath(true)
            customLibraryPassedToTheCompilerByTheAbsolutePath(false)
        }

        private fun NonRepeatedModuleNameGenerator.customLibraryPassedToTheCompilerByTheAbsolutePath(produceUnpackedKlib: Boolean) {
            val dependencyFile = compileDependencyModule(produceUnpackedKlib)

            sequenceOf(
                dependencyFile.absolutePath,
                runIf(produceUnpackedKlib) { dependencyFile.resolve("../${dependencyFile.name}").absolutePath },
                runIf(produceUnpackedKlib) { dependencyFile.resolve("default/..").absolutePath },
            ).filterNotNull().forEach { libraryPath ->
                val alternativeLibraryPath = if (produceUnpackedKlib) "$libraryPath.klib" else libraryPath.removeSuffix(".klib")

                compileMainModule(libraryPath)
                expectFailingAsNotFound(alternativeLibraryPath) { compileMainModule(alternativeLibraryPath) }
            }
        }

        @Test
        @DisplayName("Custom library passed to the compiler by the relative path")
        fun testCustomLibraryPassedToTheCompilerByTheRelativePath() = with(NonRepeatedModuleNameGenerator()) {
            customLibraryPassedToTheCompilerByTheRelativePath(true, false)
            customLibraryPassedToTheCompilerByTheRelativePath(false, false)
            customLibraryPassedToTheCompilerByTheRelativePath(true, true)
            customLibraryPassedToTheCompilerByTheRelativePath(false, true)
        }

        private fun NonRepeatedModuleNameGenerator.customLibraryPassedToTheCompilerByTheRelativePath(
            produceUnpackedKlib: Boolean,
            relocateDependencyToAnotherDir: Boolean,
        ) {
            val dependencyFileName = run {
                val originalDependencyFile = compileDependencyModule(produceUnpackedKlib)
                if (relocateDependencyToAnotherDir) {
                    val customDir = buildDir.resolve("custom-dir-for-${if (produceUnpackedKlib) "unpacked" else "packed"}-library")
                    customDir.mkdirs()

                    val movedDependencyFile = customDir.resolve(originalDependencyFile.name)
                    originalDependencyFile.copyRecursively(movedDependencyFile)
                    originalDependencyFile.deleteRecursively()

                    movedDependencyFile.name
                } else {
                    originalDependencyFile.name
                }
            }

            sequenceOf(
                dependencyFileName,
                runIf(produceUnpackedKlib) { "$dependencyFileName/../$dependencyFileName" },
                runIf(produceUnpackedKlib) { "$dependencyFileName/default/.." },
            ).filterNotNull().forEach { libraryPath ->
                if (relocateDependencyToAnotherDir)
                    expectFailingAsNotFound(libraryPath) { compileMainModule(libraryPath) }
                else
                    compileMainModule(libraryPath)
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
            try {
                block()
                fail("Normally should not get here")
            } catch (cte: CompilationToolException) {
                assertTrue(cte.reason.contains("error: KLIB resolver: Could not find \"$dependencyNameInError\""))
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
            val module = createModules(Module(moduleNames.uniqueName)).single()

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

        private fun File.readLibrary(): KotlinLibrary = resolveSingleFileKlib(KFile(absolutePath))

        private val KotlinLibrary.dependencies: Set<String>
            get() = manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).toSet()
    }

    private fun testIrProvidersMismatchImpl(srcDir: File, freeCompilerArgs: TestCompilerArgs) {
        val testCaseKlib = generateTestCaseWithSingleModule(srcDir.resolve("empty.kt"), freeCompilerArgs)
        val klibResult = LibraryCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCaseKlib.freeCompilerArgs,
            sourceModules = testCaseKlib.modules,
            dependencies = listOf(),
            expectedArtifact = getLibraryArtifact(testCaseKlib, buildDir)
        ).result.assertSuccess()

        val testCase = generateTestCaseWithSingleFile(
            sourceFile = srcDir.resolve("irProvidersMismatch.kt"),
            testKind = TestKind.STANDALONE_NO_TR,
            extras = TestCase.NoTestRunnerExtras("main"),
        )
        // Compile test, NOT respecting possible `mode=TWO_STAGE_MULTI_MODULE`: don't add intermediate LibraryCompilation(kt->klib).
        // KT-66014: Extract this test from usual Native test run, and run it in scope of new test module
        val executableResult =
            compileToExecutableInOneStage(testCase, klibResult.resultingArtifact.asLibraryDependency()).assertSuccess()
        val testExecutable = TestExecutable(
            executableResult.resultingArtifact,
            executableResult.loggedData,
            listOf(TestName("testIrProvidersMismatch"))
        )
        runExecutableAndVerify(testCase, testExecutable)
    }

    private fun createModules(vararg modules: Module): List<Module> {
        val mapping: Map<String, Module> = modules.groupBy(Module::name).mapValues {
            it.value.singleOrNull() ?: error("Duplicated modules: ${it.value}")
        }

        modules.forEach { it.initDependencies(mapping::getValue) }

        val generatedSourcesDir = buildDir.resolve("generated-sources")
        generatedSourcesDir.mkdirs()

        modules.forEach { module ->
            module.sourceFile = generatedSourcesDir.resolve(module.name + ".kt")
            module.sourceFile.writeText(
                buildString {
                    appendLine("package ${module.name}")
                    appendLine()
                    appendLine("fun ${module.name}(indent: Int) {")
                    appendLine("    repeat(indent) { print(\"  \") }")
                    appendLine("    println(\"${module.name}\")")
                    module.dependencyNames.forEach { dependencyName ->
                        appendLine("    $dependencyName.$dependencyName(indent + 1)")
                    }
                    appendLine("}")
                }
            )
        }

        return modules.asList()
    }

    private fun List<Module>.compileModules(
        produceUnpackedKlibs: Boolean,
        useLibraryNamesInCliArguments: Boolean,
        extraCmdLineParams: List<String> = emptyList(),
        transform: ((module: Module, successKlib: TestCompilationResult.Success<out KLIB>) -> Unit)? = null
    ) {
        val klibFilesDir = buildDir.resolve(
            listOf(
                "klib-files",
                if (produceUnpackedKlibs) "unpacked" else "packed",
                if (useLibraryNamesInCliArguments) "names" else "paths",
                if (transform != null) "transformed" else "non-transformed"
            ).joinToString(".")
        )
        klibFilesDir.mkdirs()

        fun Module.computeArtifactPath(): String {
            val basePath: String = if (useLibraryNamesInCliArguments) name else klibFilesDir.resolve(name).path
            return if (produceUnpackedKlibs) basePath else "$basePath.klib"
        }

        runWithCustomWorkingDir(klibFilesDir) {
            forEach { module ->
                val testCase = generateTestCaseWithSingleFile(
                    sourceFile = module.sourceFile,
                    moduleName = module.name,
                    TestCompilerArgs(
                        buildList {
                            if (produceUnpackedKlibs) add("-nopack")
                            module.dependencies.forEach { dependency ->
                                add("-l")
                                add(dependency.computeArtifactPath())
                            }
                            addAll(extraCmdLineParams)
                        }
                    )
                )

                val compilation = LibraryCompilation(
                    settings = testRunSettings,
                    freeCompilerArgs = testCase.freeCompilerArgs,
                    sourceModules = testCase.modules,
                    dependencies = emptySet(),
                    expectedArtifact = KLIB(klibFilesDir.resolve(module.computeArtifactPath()))
                )

                val success = compilation.result.assertSuccess()
                transform?.invoke(module, success)
            }
        }
    }

    private inline fun runWithCustomWorkingDir(customWorkingDir: File, block: () -> Unit) {
        val previousWorkingDir: String = System.getProperty(USER_DIR)
        try {
            System.setProperty(USER_DIR, customWorkingDir.absolutePath)
            block()
        } finally {
            System.setProperty(USER_DIR, previousWorkingDir)
        }
    }

    /**
     * Helps to avoid generating modules with same names and paths when [compileModules] is called repetitively
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
        private val irProvidersMismatchSrcDir = File("native/native.tests/testData/irProvidersMismatch")

        private const val DUPLICATED_UNIQUE_NAME = "DUPLICATED_UNIQUE_NAME"

        private fun DuplicatedUniqueNameStrategy.asCliArgument(): String {
            return CommonKlibBasedCompilerArguments::duplicatedUniqueNameStrategy.cliArgument(alias)
        }
    }
}
