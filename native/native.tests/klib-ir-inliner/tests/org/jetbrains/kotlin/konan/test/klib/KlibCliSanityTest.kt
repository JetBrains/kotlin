/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.klib

import com.intellij.openapi.util.text.StringUtil.convertLineSeparators
import org.jetbrains.kotlin.backend.common.signatureIndex
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.support.LoggedData
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ObjCFrameworkCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KLIB_PROPERTY_IR_PROVIDER
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.metadata.parsePackageFragment
import org.jetbrains.kotlin.library.packageFqName
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.utils.patchManifestAsMap
import org.jetbrains.kotlin.test.utils.patchManifestToBumpAbiVersion
import org.jetbrains.kotlin.testFederation.HostDependent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.reflect.KProperty1

@Tag("klib")
@HostDependent
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
            (modules.modules[0] as RegularKlibTestSourceModule).sourceFile.parent.resolve("non-existent-klib").absolutePathString(),
            (modules.modules[0] as RegularKlibTestSourceModule).sourceFile.parent.resolve("non-existent-klib.klib").absolutePathString(),
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
            if (!cte.reason.contains("KLIB loader: Incompatible ABI version 3.5.0 in library: $libraryPath"))
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
    fun `Included libs are forbidden on 1st phase`() {
        var aPath: String? = null
        newSourceModules { addRegularModule("a") }.compileToKlibsViaCli { _, successKlib ->
            aPath = successKlib.resultingArtifact.klibFile.absolutePath
        }
        requireNotNull(aPath)

        try {
            newSourceModules { addRegularModule("b") }.compileToKlibsViaCli(
                extraCliArgs = listOf(K2NativeCompilerArguments::includes.cliArgument(aPath))
            )
            fail { "Normally unreachable code" }
        } catch (cte: CompilationToolException) {
            if (!cte.reason.contains("-Xinclude CLI argument is not supported when producing library"))
                throw cte
        }
    }

    @Test
    fun `Exported libs are forbidden on 1st phase`() {
        var aPath: String? = null
        newSourceModules { addRegularModule("a") }.compileToKlibsViaCli { _, successKlib ->
            aPath = successKlib.resultingArtifact.klibFile.absolutePath
        }
        requireNotNull(aPath)

        try {
            newSourceModules { addRegularModule("b") }.compileToKlibsViaCli(
                extraCliArgs = listOf(K2NativeCompilerArguments::exportedLibraries.cliArgument(aPath))
            )
            fail { "Normally unreachable code" }
        } catch (cte: CompilationToolException) {
            if (!cte.reason.contains("-Xexport-library CLI argument is only supported when producing frameworks or native libraries"))
                throw cte
        }
    }

    @Test
    fun `Compiler rejects libraries from the distribution and C-interop libraries as included libs`() {
        doTestCompilerRejectsLibrariesFromDistributionAndCInteropLibraries(testedCliParameter = K2NativeCompilerArguments::includes) { cliArgs ->
            val executableFile = buildDir.resolve("kexe.kexe").also { it.delete() }
            val compilation = ExecutableCompilation(
                settings = testRunSettings,
                freeCompilerArgs = TestCompilerArgs(cliArgs),
                sourceModules = emptyList(),
                extras = TestCase.NoTestRunnerExtras(entryPoint = "r.main"),
                dependencies = emptySet(),
                expectedArtifact = TestCompilationArtifact.Executable(executableFile),
            )
            compilation.result
        }
    }

    @Test
    fun `Compiler rejects libraries from the distribution and C-interop libraries as exported libs`() {
        // Frameworks can be built only on Apple.
        assumeTrue(testRunSettings.get<KotlinNativeTargets>().hostTarget.family.isAppleFamily)
        doTestCompilerRejectsLibrariesFromDistributionAndCInteropLibraries(testedCliParameter = K2NativeCompilerArguments::exportedLibraries) { cliArgs ->
            val frameworkDir = buildDir.resolve("frameworkDir").also { it.deleteRecursively() }
            val compilation = ObjCFrameworkCompilation(
                settings = testRunSettings,
                freeCompilerArgs = TestCompilerArgs(cliArgs),
                sourceModules = emptyList(),
                dependencies = emptySet(),
                expectedArtifact = TestCompilationArtifact.ObjCFramework(frameworkDir, "myFramework"),
            )
            compilation.result
        }
    }

    private fun doTestCompilerRejectsLibrariesFromDistributionAndCInteropLibraries(
        testedCliParameter: KProperty1<K2NativeCompilerArguments, Array<String>>,
        run2StageCompilation: (cliArgs: List<String>) -> TestCompilationResult<*>,
    ) {
        val librariesDir = testRunSettings.get<KotlinNativeHome>().librariesDir
        val target = testRunSettings.get<KotlinNativeTargets>().testTarget

        val stdlibPath: String = librariesDir.resolve("common/stdlib").absolutePath
        val posixPath: String = librariesDir.resolve("platform/${target.name}/org.jetbrains.kotlin.native.platform.posix").absolutePath

        var regularLibPath: String? = null
        var cinteropLibPath: String? = null

        newSourceModules {
            addRegularModule("r") { sourceFileAddend("fun main() = Unit") }
            addCInteropModule("c")
        }.compileToKlibsViaCli { module, successKlib ->
            when (module.name) {
                "r" -> regularLibPath = successKlib.resultingArtifact.klibFile.absolutePath
                "c" -> cinteropLibPath = successKlib.resultingArtifact.klibFile.absolutePath
            }
        }

        checkNotNull(regularLibPath)
        checkNotNull(cinteropLibPath)

        // try it with different combinations of "-no..." flags:
        listOf(
            emptyList(),
            listOf("-nostdlib"),
            listOf("-no-default-libs"),
            listOf("-nostdlib", "-no-default-libs"),
        ).forEach { extraNoSmthFlags ->
            val compilerOutput = run2StageCompilation(
                buildList {
                    this += extraNoSmthFlags
                    this += listOf("-l", stdlibPath)
                    this += listOf("-l", posixPath)
                    this += listOf("-l", regularLibPath)
                    this += listOf("-l", cinteropLibPath)
                    this += testedCliParameter.cliArgument(stdlibPath)
                    this += testedCliParameter.cliArgument(posixPath)
                    this += testedCliParameter.cliArgument(regularLibPath)
                    this += testedCliParameter.cliArgument(cinteropLibPath)
                }
            )

            compilerOutput.assertProblematicIncludedOrExportedLibs(
                cliParameter = testedCliParameter,
                stdlibPath, posixPath, cinteropLibPath
            )
        }
    }

    @Test
    fun `Compiler reports cyclic dependency between KLIBs on 2nd phase`() {
        val moduleNameToKlibPath: MutableMap</* name of test module */ String, /* original path of KLIB */ String> = linkedMapOf()

        /*
         * a <-- b <-- c
         */
        newSourceModules {
            addRegularModule("a")
            addRegularModule("b") { dependsOn("a") }
            addRegularModule("c") { dependsOn("b") }
        }.compileToKlibsViaCli { module, successKlib ->
            moduleNameToKlibPath[module.name] = successKlib.resultingArtifact.klibFile.path
        }
        assertEquals(3, moduleNameToKlibPath.size)

        /*
         * Now adding one more edge between a and c:
         * +-- a <-- b <-- c <--+
         * |                    |
         * +--------------------+
         */
        newSourceModules {
            addRegularModule("a") {
                sourceFileAddend("fun a2() = c.c(0)\nfun main() = Unit")
            }
        }.compileToKlibsViaCli(
            extraCliArgs = listOf(CLI_PARAM_LIBRARIES, moduleNameToKlibPath.getValue("c"))
        ) { module, successKlib ->
            assertEquals("a", module.name)
            moduleNameToKlibPath["a"] = successKlib.resultingArtifact.klibFile.path
        }
        assertEquals(3, moduleNameToKlibPath.size)

        val compilationResult = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = TestCompilerArgs(
                buildList {
                    for (klibPath in moduleNameToKlibPath.values) this += listOf(CLI_PARAM_LIBRARIES, klibPath)
                    this += K2NativeCompilerArguments::includes.cliArgument(moduleNameToKlibPath.getValue("a"))
                }
            ),
            sourceModules = emptyList(),
            extras = TestCase.NoTestRunnerExtras(entryPoint = "a.main"),
            dependencies = emptySet(),
            expectedArtifact = TestCompilationArtifact.Executable(buildDir.resolve("kexe.kexe")),
        ).result

        val compilationToolCall = (compilationResult as TestCompilationResult.CompilationToolFailure).loggedData
        assertEquals(ExitCode.COMPILATION_ERROR, compilationToolCall.exitCode)

        // Note: The cycle is reported starting from the first library in the load order, i.e. in the order of `-library` arguments.
        // a depends on c, c depends on b, b depends on a.
        val expectedDiagnostic = """
            error: cyclic dependency detected while computing DAG of KLIB dependencies:
            ${moduleNameToKlibPath.getValue("a")} ->
            ${moduleNameToKlibPath.getValue("c")} ->
            ${moduleNameToKlibPath.getValue("b")} ->
            ${moduleNameToKlibPath.getValue("a")}
        """.trimIndent()

        assertTrue(expectedDiagnostic in convertLineSeparators(compilationToolCall.toolOutput)) {
            "[$expectedDiagnostic] is not contained in [${compilationToolCall.toolOutput}]"
        }
    }

    @Test
    fun `Compiler warns about clashing declarations on 2nd phase`() {
        /*
         * A, B and C are three different libraries with the same package `lib`. The clashing declarations:
         * - `lib.lib()` is declared in A, B and C
         * - `lib.ab()` is declared in A and B
         * - `lib.ac()` is declared in A and C
         * - `lib.bc()` is declared in B and C
         */
        val libraryNameToKlibPath: Map</* name of library */ String, /* path of KLIB */ Path> = mapOf(
            "A" to "fun ab() = 1\nfun ac() = 1",
            "B" to "fun ab() = 2\nfun bc() = 2",
            "C" to "fun ac() = 3\nfun bc() = 3",
        ).mapValues { [libraryName, clashingDeclarations] ->
            var klibPath: Path? = null

            // Use separate directories to not overwrite the sources and the KLIBs of other libraries.
            // Use different module names to make these libraries different, although they have the same package.
            newSourceModules(customGeneratedSourcesDir = buildDir.resolve("generated-sources-$libraryName").toPath()) {
                addRegularModule("lib") { sourceFileAddend(clashingDeclarations) }
            }.compileToKlibsViaCli(
                extraCliArgs = listOf("-module-name", "lib$libraryName"),
                customGeneratedKlibsDir = buildDir.resolve("path/to/$libraryName").toPath(),
            ) { _, successKlib ->
                klibPath = successKlib.resultingArtifact.klibFile.toPath().absolute()
            }

            checkNotNull(klibPath)
        }

        var mainKlibPath: String? = null
        newSourceModules {
            addRegularModule("main") { sourceFileAddend("fun main() = Unit") }
        }.compileToKlibsViaCli { _, successKlib ->
            mainKlibPath = successKlib.resultingArtifact.klibFile.absolutePath
        }
        checkNotNull(mainKlibPath)

        fun doCheck(
            loadOrder: List</* name of library */ String>,
            expectedWarnings: List<String>,
        ) {
            val compilationResult = ExecutableCompilation(
                settings = testRunSettings,
                freeCompilerArgs = TestCompilerArgs(
                    buildList {
                        for (libraryName in loadOrder) this += listOf(CLI_PARAM_LIBRARIES, libraryNameToKlibPath.getValue(libraryName).pathString)
                        this += listOf(CLI_PARAM_LIBRARIES, mainKlibPath)
                        this += K2NativeCompilerArguments::includes.cliArgument(mainKlibPath)
                    }
                ),
                sourceModules = emptyList(),
                extras = TestCase.NoTestRunnerExtras(entryPoint = "main.main"),
                dependencies = emptySet(),
                expectedArtifact = TestCompilationArtifact.Executable(buildDir.resolve("kexe-${loadOrder.joinToString("")}.kexe")),
            ).result

            val compilationToolCall = (compilationResult as TestCompilationResult.ImmediateResult<*>).loggedData
                    as LoggedData.CompilationToolCall
            assertEquals(ExitCode.OK, compilationToolCall.exitCode) { compilationToolCall.toolOutput }

            val signatureStrippingRegex = Regex("""'([^'|]+)\|[^']*'""")

            val actualWarnings = compilationToolCall.toolOutput.lines()
                .filter { it.startsWith("warning: libraries ") }
                .map { warning ->
                    // Library paths are machine-specific; replace them with stable relative paths before comparison.
                    libraryNameToKlibPath.values.fold(warning) { result, path ->
                        result.replace(
                            path.pathString,
                            convertLineSeparators(path.relativeTo(buildDir.toPath()).pathString),
                        )
                    }.replace(signatureStrippingRegex, "'$1'")
                }

            assertEquals(expectedWarnings, actualWarnings) { actualWarnings.joinToString("\n") }
        }

        // Note: The warnings are sorted lexicographically by the paths of the libraries that declare the clashing declarations.
        doCheck(
            loadOrder = listOf("A", "B", "C"),
            expectedWarnings = listOf(
                "warning: libraries path/to/A/lib and path/to/B/lib declare 1 top-level declaration with the same signature: 'lib/ab'. " +
                        "Declarations from path/to/A/lib are used because it comes first in the load order; the duplicates from path/to/B/lib are ignored. " +
                        "To avoid ambiguity, remove the duplicate libraries from the dependencies or make sure that the library with the intended declarations is listed first.",
                "warning: libraries path/to/A/lib, path/to/B/lib and path/to/C/lib declare 1 top-level declaration with the same signature: 'lib/lib'. " +
                        "Declarations from path/to/A/lib are used because it comes first in the load order; the duplicates from path/to/B/lib and path/to/C/lib are ignored. " +
                        "To avoid ambiguity, remove the duplicate libraries from the dependencies or make sure that the library with the intended declarations is listed first.",
                "warning: libraries path/to/A/lib and path/to/C/lib declare 1 top-level declaration with the same signature: 'lib/ac'. " +
                        "Declarations from path/to/A/lib are used because it comes first in the load order; the duplicates from path/to/C/lib are ignored. " +
                        "To avoid ambiguity, remove the duplicate libraries from the dependencies or make sure that the library with the intended declarations is listed first.",
                "warning: libraries path/to/B/lib and path/to/C/lib declare 1 top-level declaration with the same signature: 'lib/bc'. " +
                        "Declarations from path/to/B/lib are used because it comes first in the load order; the duplicates from path/to/C/lib are ignored. " +
                        "To avoid ambiguity, remove the duplicate libraries from the dependencies or make sure that the library with the intended declarations is listed first.",
            )
        )
        doCheck(
            loadOrder = listOf("C", "B", "A"),
            expectedWarnings = listOf(
                "warning: libraries path/to/B/lib and path/to/A/lib declare 1 top-level declaration with the same signature: 'lib/ab'. " +
                        "Declarations from path/to/B/lib are used because it comes first in the load order; the duplicates from path/to/A/lib are ignored. " +
                        "To avoid ambiguity, remove the duplicate libraries from the dependencies or make sure that the library with the intended declarations is listed first.",
                "warning: libraries path/to/C/lib and path/to/A/lib declare 1 top-level declaration with the same signature: 'lib/ac'. " +
                        "Declarations from path/to/C/lib are used because it comes first in the load order; the duplicates from path/to/A/lib are ignored. " +
                        "To avoid ambiguity, remove the duplicate libraries from the dependencies or make sure that the library with the intended declarations is listed first.",
                "warning: libraries path/to/C/lib and path/to/B/lib declare 1 top-level declaration with the same signature: 'lib/bc'. " +
                        "Declarations from path/to/C/lib are used because it comes first in the load order; the duplicates from path/to/B/lib are ignored. " +
                        "To avoid ambiguity, remove the duplicate libraries from the dependencies or make sure that the library with the intended declarations is listed first.",
                "warning: libraries path/to/C/lib, path/to/B/lib and path/to/A/lib declare 1 top-level declaration with the same signature: 'lib/lib'. " +
                        "Declarations from path/to/C/lib are used because it comes first in the load order; the duplicates from path/to/B/lib and path/to/A/lib are ignored. " +
                        "To avoid ambiguity, remove the duplicate libraries from the dependencies or make sure that the library with the intended declarations is listed first.",
            )
        )
        doCheck(
            loadOrder = listOf("B", "C", "A"),
            expectedWarnings = listOf(
                "warning: libraries path/to/B/lib and path/to/A/lib declare 1 top-level declaration with the same signature: 'lib/ab'. " +
                        "Declarations from path/to/B/lib are used because it comes first in the load order; the duplicates from path/to/A/lib are ignored. " +
                        "To avoid ambiguity, remove the duplicate libraries from the dependencies or make sure that the library with the intended declarations is listed first.",
                "warning: libraries path/to/B/lib and path/to/C/lib declare 1 top-level declaration with the same signature: 'lib/bc'. " +
                        "Declarations from path/to/B/lib are used because it comes first in the load order; the duplicates from path/to/C/lib are ignored. " +
                        "To avoid ambiguity, remove the duplicate libraries from the dependencies or make sure that the library with the intended declarations is listed first.",
                "warning: libraries path/to/B/lib, path/to/C/lib and path/to/A/lib declare 1 top-level declaration with the same signature: 'lib/lib'. " +
                        "Declarations from path/to/B/lib are used because it comes first in the load order; the duplicates from path/to/C/lib and path/to/A/lib are ignored. " +
                        "To avoid ambiguity, remove the duplicate libraries from the dependencies or make sure that the library with the intended declarations is listed first.",
                "warning: libraries path/to/C/lib and path/to/A/lib declare 1 top-level declaration with the same signature: 'lib/ac'. " +
                        "Declarations from path/to/C/lib are used because it comes first in the load order; the duplicates from path/to/A/lib are ignored. " +
                        "To avoid ambiguity, remove the duplicate libraries from the dependencies or make sure that the library with the intended declarations is listed first.",
            )
        )
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

    @Test
    fun `Compiler does not load platform libraries implicitly when -no-default-libs is passed`() {
        try {
            newSourceModules {
                addRegularModule("a") {
                    sourceFileAddend(
                        """
                            @kotlinx.cinterop.ExperimentalForeignApi
                            fun callPosix() = platform.posix.fopen("test.txt", "r")
                        """.trimIndent()
                    )
                }
            }.compileToKlibsViaCli(extraCliArgs = listOf("-no-default-libs"))

            fail { "CompilationToolException is expected" }
        } catch (cte: CompilationToolException) {
            if (!cte.reason.contains("error: unresolved reference 'platform'"))
                throw cte
        }
    }

    @Test
    fun `Compiler fails on missing direct dependency`() {
        fun compile(passA: Boolean) = newSourceModules {
            addRegularModule("a") { sourceFileAddend("fun foo() = 42") }
            addRegularModule("b") {
                if (passA) dependsOn("a")
                sourceFileAddend("fun useA() = a.foo()")
            }
        }.compileToKlibsViaCli()

        compile(passA = true)

        try {
            compile(passA = false)

            fail { "CompilationToolException is expected" }
        } catch (cte: CompilationToolException) {
            if (!cte.reason.contains("error: unresolved reference 'a'"))
                throw cte
        }
    }

    @Test
    fun `Klibs in distribution have signature index`() {
        val libraries = KlibLoader {
            libraryProviders(
                KlibNativeDistributionLibraryProvider(testRunSettings.get<KotlinNativeHome>().dir) {
                    withStdlib()
                    withPlatformLibs(testRunSettings.get<KotlinNativeTargets>().testTarget)
                }
            )
        }.load().librariesStdlibFirst

        assertTrue(libraries.size > 1)
        assertTrue(libraries.any { it.isNativeStdlib })

        for (library in libraries) {
            val signatureIndex = library.signatureIndex ?: fail("No signature index in library: ${library.canonicalPath}")

            if (signatureIndex.exportedTopLevelSignatures.isEmpty()) {
                // There are a few special C-interop libraries without declarations. Need to check this additionally.
                assertTrue(library.isCInteropLibrary())

                val packageFqName = library.packageFqName!!
                val metadata = library.metadata

                for (fragmentName in metadata.getPackageFragmentNames(packageFqName)) {
                    val packageFragmentProto = parsePackageFragment(metadata.getPackageFragment(packageFqName, fragmentName))
                    assertEquals(0, packageFragmentProto.class_Count)
                    assertEquals(0, packageFragmentProto.`package`?.functionCount ?: 0)
                    assertEquals(0, packageFragmentProto.`package`?.propertyCount ?: 0)
                    assertEquals(0, packageFragmentProto.`package`?.typeAliasCount ?: 0)
                }
            } else {
                assertTrue(signatureIndex.importedTopLevelSignatures.isNotEmpty())
            }
        }
    }

    @Test
    fun `Compiler generates signature index for regular Klibs`() {
        newSourceModules {
            addRegularModule("r") { sourceFileAddend("fun main() = Unit") }
            addCInteropModule("c")
        }.compileToKlibsViaCli { _, successKlib ->
            val generatedKlibPath = successKlib.resultingArtifact.klibFile.absolutePath
            val generatedKlib = KlibLoader { libraryPaths(generatedKlibPath) }.load().librariesStdlibFirst.single()

            val signatureIndex = generatedKlib.signatureIndex ?: fail("No signature index in library: $generatedKlibPath")
            assertTrue(signatureIndex.exportedTopLevelSignatures.isNotEmpty())
            assertTrue(signatureIndex.importedTopLevelSignatures.isNotEmpty())
        }
    }

    @Test
    fun `Compiler does not generate signature index for metadata Klibs`() {
        newSourceModules {
            addRegularModule("r") { sourceFileAddend("fun main() = Unit") }
        }.compileToKlibsViaCli(extraCliArgs = listOf("-Xmetadata-klib")) { _, successKlib ->
            val generatedKlibPath = successKlib.resultingArtifact.klibFile.absolutePath
            val generatedKlib = KlibLoader { libraryPaths(generatedKlibPath) }.load().librariesStdlibFirst.single()
            assertNull(generatedKlib.signatureIndex) {
                "Unexpected signature index in metadata-only library: $generatedKlibPath"
            }
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

    private fun TestCompilationResult<*>.assertProblematicIncludedOrExportedLibs(
        cliParameter: KProperty1<K2NativeCompilerArguments, *>,
        vararg pathsOfExpectedProblematicIncludes: String,
    ) {
        val compilationToolCall = (this as TestCompilationResult.ImmediateResult<*>).loggedData as LoggedData.CompilationToolCall

        val messagePattern = "cannot be used in ${cliParameter.cliArgument} CLI argument: "
        val toolOutput = compilationToolCall.toolOutput.lines()

        val pathsOfActualProblematicIncludes = toolOutput.mapNotNull { line ->
            if (messagePattern in line)
                line.substringAfterLast(messagePattern).trim()
            else
                null
        }

        assertEquals(pathsOfExpectedProblematicIncludes.toSet(), pathsOfActualProblematicIncludes.toSet())
    }

    companion object {
        private val CLI_PARAM_LIBRARIES: String = K2NativeCompilerArguments::libraries.cliArgument
        private val CLI_PARAM_FRIENDS: String = K2NativeCompilerArguments::friendModules.cliArgument
    }
}
