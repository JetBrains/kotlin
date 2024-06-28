/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.TestName
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDENCY_VERSION
import org.jetbrains.kotlin.library.KLIB_PROPERTY_LIBRARY_VERSION
import org.jetbrains.kotlin.library.SearchPathResolver
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.utils.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import java.io.File

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
            ) { module, klib ->
                if (module.name == "lib1") {
                    patchManifestToBumpAbiVersion(JUnit5Assertions, klib.klibFile)
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
        ) { module, klib ->
            when (module.name) {
                "liba" -> {
                    // set the library version = 1.0
                    patchManifestAsMap(JUnit5Assertions, klib.klibFile) { properties ->
                        @Suppress("DEPRECATION")
                        properties[KLIB_PROPERTY_LIBRARY_VERSION] = "1.0"
                    }
                }
                "libb" -> {
                    // pretend it depends on liba v2.0
                    patchManifestAsMap(JUnit5Assertions, klib.klibFile) { properties ->
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
        ) { module, klib ->
            when (module.name) {
                "liba" -> {
                    // set the library version = 1.0
                    patchManifestAsMap(JUnit5Assertions, klib.klibFile) { properties ->
                        @Suppress("DEPRECATION")
                        properties[KLIB_PROPERTY_LIBRARY_VERSION] = "1.0"
                    }
                }
                "libb" -> {
                    // check that dependency version is set
                    patchManifestAsMap(JUnit5Assertions, klib.klibFile) { properties ->
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
        transform: ((module: Module, klib: KLIB) -> Unit)? = null
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

                val klib = compilation.result.assertSuccess().resultingArtifact
                transform?.invoke(module, klib)
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

    companion object {
        private const val USER_DIR = "user.dir"
        private val irProvidersMismatchSrcDir = File("native/native.tests/testData/irProvidersMismatch")
    }
}
