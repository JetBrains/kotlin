/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_IR_PROVIDER
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.utils.patchManifestAsMap
import org.jetbrains.kotlin.test.utils.patchManifestToBumpAbiVersion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.collections.set
import kotlin.text.contains

@Tag("klib")
class KlibCliSanityTest : AbstractNativeSimpleTest() {
    @Test
    fun `Compiler consumes unpacked KLIBs passed by absolute paths via CLI arguments`() {
        val modules = newSourceModules {
            addRegularModule("a")
            addRegularModule("b") { dependsOn("a") }
            addRegularModule("c") { dependsOn("a") }
            addRegularModule("d") { dependsOn("b", "c", "a") }
        }

        modules.compileToKlibsViaCli { _, successKlib ->
            successKlib.assertNoKlibLoaderIssues()
        }
    }

    @Test
    fun `Compiler consumes packed KLIBs passed by absolute paths via CLI arguments`() {
        val modules = newSourceModules {
            addRegularModule("a")
            addRegularModule("b") { dependsOn("a") }
            addRegularModule("c") { dependsOn("a") }
            addRegularModule("d") { dependsOn("b", "c", "a") }
        }

        modules.compileToKlibsViaCli(produceUnpackedKlibs = false) { _, successKlib ->
            successKlib.assertNoKlibLoaderIssues()
        }
    }

    @Test
    fun `Compiler consumes unpacked KLIBs passed by relative paths via CLI arguments`() {
        doTestCompilerConsumesKlibPassedByRelativePath(produceUnpackedKlibs = true)
    }

    @Test
    fun `Compiler consumes packed KLIBs passed by relative paths via CLI arguments`() {
        doTestCompilerConsumesKlibPassedByRelativePath(produceUnpackedKlibs = false)
    }

    private fun doTestCompilerConsumesKlibPassedByRelativePath(produceUnpackedKlibs: Boolean) {
        var moduleAKlibFile: File? = null

        newSourceModules {
            addRegularModule("a")
        }.compileToKlibsViaCli(produceUnpackedKlibs = produceUnpackedKlibs) { _, successKlib ->
            moduleAKlibFile = successKlib.resultingArtifact.klibFile
        }
        checkNotNull(moduleAKlibFile)

        val moduleAKlibRelativePath: String = moduleAKlibFile.relativeTo(File(System.getProperty("user.dir"))).path
        assertNotEquals(moduleAKlibRelativePath, moduleAKlibFile.path)

        newSourceModules {
            addRegularModule("b") {
                sourceFileAddend("fun foo() = a.a(0)") // call a real function from "a"
            }
        }.compileToKlibsViaCli(extraCliArgs = listOf(CLI_PARAM_LIBRARIES, moduleAKlibRelativePath)) { _, successKlib ->
            successKlib.assertNoKlibLoaderIssues()
        }
    }

    @Test
    fun `Compiler warns on non-existent KLIB passed via CLI arguments`() {
        val modules = newSourceModules { addRegularModule("a") }

        listOf(
            "non-existent-klib",
            "non-existent-klib.klib",
            "non-existent-dir/non-existent-klib",
            "non-existent-dir/non-existent-klib.klib",
            modules.modules[0].sourceFile.parentFile.resolve("non-existent-klib").absolutePath,
            modules.modules[0].sourceFile.parentFile.resolve("non-existent-klib.klib").absolutePath,
        ).forEach { libraryPath ->
            modules.compileToKlibsViaCli(
                extraCliArgs = listOf(
                    CLI_PARAM_LIBRARIES, libraryPath,
                )
            ) { _, successKlib ->
                successKlib.assertLibraryNotFound(libraryPath)
            }

            modules.compileToKlibsViaCli(
                extraCliArgs = listOf(
                    CLI_PARAM_LIBRARIES, libraryPath,
                    CLI_PARAM_FRIENDS, libraryPath,
                )
            ) { _, successKlib ->
                successKlib.assertLibraryNotFound(libraryPath)
            }

            modules.compileToKlibsViaCli(
                extraCliArgs = listOf(
                    CLI_PARAM_LIBRARIES, libraryPath,
                    CLI_ARG_INCLUDES(libraryPath),
                )
            ) { _, successKlib ->
                successKlib.assertLibraryNotFound(libraryPath)
            }
        }
    }

    @Test
    fun `Compiler warns on unexpected friend KLIB passed via CLI arguments`() {
        var moduleAKlibPath: String? = null

        newSourceModules {
            addRegularModule("a")
        }.compileToKlibsViaCli { _, successKlib ->
            moduleAKlibPath = successKlib.resultingArtifact.klibFile.path
        }
        checkNotNull(moduleAKlibPath)

        val moduleB = newSourceModules {
            addRegularModule("b")
        }

        // Existing friend that is also passed via `-library`.
        moduleB.compileToKlibsViaCli(
            extraCliArgs = listOf(
                CLI_PARAM_LIBRARIES, moduleAKlibPath,
                CLI_PARAM_FRIENDS, moduleAKlibPath,
            )
        ) { _, successKlib ->
            successKlib.assertNoKlibLoaderIssues()
            successKlib.assertNoFriendIssues()
        }

        // Existing friend that is not passed via `-library`.
        moduleB.compileToKlibsViaCli(
            extraCliArgs = listOf(
                CLI_PARAM_FRIENDS, moduleAKlibPath,
            )
        ) { _, successKlib ->
            successKlib.assertNoKlibLoaderIssues()
            successKlib.assertUnexpectedFriends(moduleAKlibPath)
        }

        val nonExistentKlibPath = "non-existent-klib.klib"

        // Non-existing friend that is also passed via `-library`.
        moduleB.compileToKlibsViaCli(
            extraCliArgs = listOf(
                CLI_PARAM_LIBRARIES, nonExistentKlibPath,
                CLI_PARAM_FRIENDS, nonExistentKlibPath,
            )
        ) { _, successKlib ->
            successKlib.assertLibraryNotFound(nonExistentKlibPath)
            successKlib.assertNoFriendIssues()
        }

        // Non-existing friend that is not passed via `-library`.
        moduleB.compileToKlibsViaCli(
            extraCliArgs = listOf(
                CLI_PARAM_FRIENDS, nonExistentKlibPath,
            )
        ) { _, successKlib ->
            successKlib.assertNoKlibLoaderIssues()
            successKlib.assertUnexpectedFriends(nonExistentKlibPath)
        }
    }

    @Test
    fun `Compiler ignores non-existent transitive dependency in depends= property of regular KLIB, v1`() =
        doTestIgnoringNonexistentTransitiveDependencyInManifestV1(lastModuleIsCInterop = false)

    @Test
    fun `Compiler ignores non-existent transitive dependency in depends= property of regular KLIB, v2`() =
        doTestIgnoringNonexistentTransitiveDependencyInManifestV2(lastModuleIsCInterop = false)

    @Test
    fun `Compiler ignores non-existent transitive dependency in depends= property of C-interop KLIB, v1`() =
        doTestIgnoringNonexistentTransitiveDependencyInManifestV1(lastModuleIsCInterop = true)

    @Test
    fun `Compiler ignores non-existent transitive dependency in depends= property of C-interop KLIB, v2`() =
        doTestIgnoringNonexistentTransitiveDependencyInManifestV2(lastModuleIsCInterop = true)

    private fun doTestIgnoringNonexistentTransitiveDependencyInManifestV1(lastModuleIsCInterop: Boolean) {
        var aKlib: KLIB? = null

        newSourceModules {
            addRegularModule("a")
            addRegularModule("b") { dependsOn("a") }
            if (lastModuleIsCInterop)
                addCInteropModule("c") { dependsOn("b") }
            else
                addRegularModule("c") { dependsOn("b") }
        }.compileToKlibsViaCli { module, successKlib ->
            when (module.name) {
                "a" -> aKlib = successKlib.resultingArtifact
                "b" -> aKlib!!.klibFile.delete() // remove transitive dependency `a`, so subsequent compilation of `c` would miss it.
                "c" -> successKlib.assertNoKlibLoaderIssues()
            }
        }
    }

    private fun doTestIgnoringNonexistentTransitiveDependencyInManifestV2(lastModuleIsCInterop: Boolean) {
        newSourceModules {
            addRegularModule("b")
            if (lastModuleIsCInterop)
                addCInteropModule("c") { dependsOn("b") }
            else
                addRegularModule("c") { dependsOn("b") }
        }.compileToKlibsViaCli { module, successKlib ->
            when (module.name) {
                "b" -> {
                    // Making cinterop add something to its `depends =` is tricky in these tests:
                    // for that, we need the same header file accessible to both `a` and `b`.
                    // Instead of overcomplicating the test engine, let's do a little shortcut:
                    patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                        properties[KLIB_PROPERTY_DEPENDS] += " a"
                    }
                    // In other words: unlike the previous test, don't compile-and-remove "a" at all,
                    // but just patch the manifest to mention it as if it existed.
                }
                "c" -> successKlib.assertNoKlibLoaderIssues()
            }
        }
    }

    @Test
    fun `Compiler rejects KLIB with unsupported ABI version`() {
        val modules = newSourceModules {
            addRegularModule("lib1")
            addRegularModule("lib2") { dependsOn("lib1") }
        }

        // Control compilation -- should finish successfully.
        modules.compileToKlibsViaCli { _, successKlib ->
            successKlib.assertNoKlibLoaderIssues()
        }

        // Compilation with patched manifest -- should fail.
        try {
            modules.compileToKlibsViaCli { module, successKlib ->
                if (module.name == "lib1") {
                    patchManifestToBumpAbiVersion(JUnit5Assertions, successKlib.resultingArtifact.klibFile)
                }
            }

            fail { "Normally unreachable code" }
        } catch (cte: CompilationToolException) {
            val libraryPath = buildDir.resolve("klib-files.unpacked.transformed/lib1").path
            if (!cte.reason.contains("KLIB loader: Incompatible ABI version 3.4.0 in library: $libraryPath"))
                throw cte
        }
    }

    @Test
    fun `Compiler consumes KLIB with known IR provider`() {
        doTestIrProviders(irProviderName = KLIB_INTEROP_IR_PROVIDER_IDENTIFIER)
    }

    @Test
    fun `Compiler rejects KLIB with unknown IR provider`() {
        try {
            doTestIrProviders(irProviderName = "QwERTY")
            fail { "Normally unreachable code" }
        } catch (cte: CompilationToolException) {
            if (!cte.reason.contains("KLIB loader: Library with unsupported IR provider QwERTY:"))
                throw cte
        }
    }

    private fun doTestIrProviders(irProviderName: String) {
        newSourceModules {
            addRegularModule("a")
            addRegularModule("b") { dependsOn("a") }
        }.compileToKlibsViaCli { module, successKlib ->
            successKlib.assertNoKlibLoaderIssues()

            if (module.name == "a") {
                patchManifestAsMap(JUnit5Assertions, successKlib.resultingArtifact.klibFile) { properties ->
                    properties[KLIB_PROPERTY_IR_PROVIDER] = irProviderName
                }
            }
        }
    }

    @Test
    fun `Compiler loads libraries bundled in Kotlin Native distribution`() {
        doTestLoadingBundledLibraries(noStdlib = false, noDefaultLibs = false)
        doTestLoadingBundledLibraries(noStdlib = true, noDefaultLibs = false)
        doTestLoadingBundledLibraries(noStdlib = false, noDefaultLibs = true)
        doTestLoadingBundledLibraries(noStdlib = true, noDefaultLibs = true)
    }

    private fun doTestLoadingBundledLibraries(noStdlib: Boolean, noDefaultLibs: Boolean) {
        val librariesDir = testRunSettings.get<KotlinNativeHome>().librariesDir
        val target = testRunSettings.get<KotlinNativeTargets>().testTarget

        val stdlibPath = librariesDir.resolve("common/stdlib").absolutePath
        val posixPath = librariesDir.resolve("platform/${target.name}/org.jetbrains.kotlin.native.platform.posix").absolutePath

        newSourceModules {
            addRegularModule("a") {
                sourceFileAddend(
                    """
                        fun callStdlib() = buildList<String> { this += "hello" }
                        
                        @kotlinx.cinterop.ExperimentalForeignApi
                        fun callPosix() = platform.posix.fopen("test.txt", "r")
                    """.trimIndent()
                )
            }
        }.compileToKlibsViaCli(
            extraCliArgs = buildList {
                if (noStdlib) this += listOf("-nostdlib", "-l", stdlibPath)
                if (noDefaultLibs) this += listOf("-no-default-libs", "-l", posixPath)
            }
        ) { _, successKlib ->
            successKlib.assertNoKlibLoaderIssues()
        }
    }

    private fun TestCompilationResult.Success<out KLIB>.assertNoKlibLoaderIssues() {
        val compilationToolCall = loggedData as LoggedData.CompilationToolCall
        assertEquals(ExitCode.OK, compilationToolCall.exitCode)

        val toolOutput = compilationToolCall.toolOutput.lineSequence()
            .filter { "KLIB loader:" in it }
            .toList()

        assertEquals(0, toolOutput.size)
    }

    private fun TestCompilationResult.Success<out KLIB>.assertLibraryNotFound(libraryPath: String) {
        val compilationToolCall = loggedData as LoggedData.CompilationToolCall
        assertEquals(ExitCode.OK, compilationToolCall.exitCode)

        val toolOutput = compilationToolCall.toolOutput.lineSequence()
            .filter { "KLIB loader:" in it }
            .toList()

        assertEquals(1, toolOutput.size)
        assertTrue("KLIB loader: Library not found: $libraryPath" in toolOutput[0])
    }

    private fun TestCompilationResult.Success<out KLIB>.assertNoFriendIssues() {
        val compilationToolCall = loggedData as LoggedData.CompilationToolCall
        assertEquals(ExitCode.OK, compilationToolCall.exitCode)

        val toolOutput = compilationToolCall.toolOutput.lineSequence()
            .filter { CLI_PARAM_FRIENDS in it && CLI_PARAM_LIBRARIES in it }
            .toList()

        assertEquals(0, toolOutput.size)
    }

    private fun TestCompilationResult.Success<out KLIB>.assertUnexpectedFriends(friendPath: String) {
        val compilationToolCall = loggedData as LoggedData.CompilationToolCall
        assertEquals(ExitCode.OK, compilationToolCall.exitCode)

        val toolOutput = compilationToolCall.toolOutput.lineSequence()
            .filter { CLI_PARAM_FRIENDS in it && CLI_PARAM_LIBRARIES in it }
            .toList()

        assertEquals(1, toolOutput.size)
        assertTrue(": $friendPath" in toolOutput[0])
    }

    companion object {
        private val CLI_PARAM_LIBRARIES: String = K2NativeCompilerArguments::libraries.cliArgument
        private val CLI_PARAM_FRIENDS: String = K2NativeCompilerArguments::friendModules.cliArgument

        @Suppress("TestFunctionName")
        private fun CLI_ARG_INCLUDES(path: String): String = K2NativeCompilerArguments::includes.cliArgument(path)
    }
}