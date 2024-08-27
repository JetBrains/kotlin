/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.utils.assertCompilerOutputHasKlibResolverIncompatibleAbiMessages
import org.jetbrains.kotlin.test.utils.patchManifestToBumpAbiVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class WasmKlibResolverTest() {
    @Test
    fun testWarningAboutRejectedLibraryIsNotSuppressed() {
        val testDataDir = File("compiler/testData/klib/resolve/mismatched-abi-version")
        val tmpdir = KtTestUtil.tmpDirForTest(this.javaClass.getSimpleName(), "testWarningAboutRejectedLibraryIsNotSuppressed")

        val lib1V1 = createKlibDir(tmpdir, "lib1", 1)

        compileKlib(
            sourceFile = testDataDir.resolve("lib1.kt"),
            dependency = null,
            outputFile = lib1V1
        ).assertSuccess() // Should compile successfully.

        compileKlib(
            sourceFile = testDataDir.resolve("lib2.kt"),
            dependency = lib1V1,
            outputFile = createKlibDir(tmpdir, "lib2", 1)
        ).assertSuccess() // Should compile successfully.

        // Now patch lib1:
        val lib1V2 = createKlibDir(tmpdir, "lib1", 2)
        lib1V1.copyRecursively(lib1V2)
        patchManifestToBumpAbiVersion(JUnit4Assertions, lib1V2)

        val result = compileKlib(
            sourceFile = testDataDir.resolve("lib2.kt"),
            dependency = lib1V2,
            outputFile = createKlibDir(tmpdir, "lib2", 2)
        )

        result.assertFailure() // Should not compile successfully.

        assertCompilerOutputHasKlibResolverIncompatibleAbiMessages(JUnit4Assertions, result.output, missingLibrary = "/v2/lib1", tmpdir)
    }

    @Test
    fun testResolvingTransitiveDependenciesRecordedInManifest() {
        val tmpdir = KtTestUtil.tmpDirForTest(this.javaClass.getSimpleName(), "testResolvingTransitiveDependenciesRecordedInManifest")
        val moduleA = Module("a")
        val moduleB = Module("b", "a")
        val moduleC = Module("c", "b")
        createModules(tmpdir, moduleA, moduleB, moduleC)


        val aKlib = tmpdir.resolve("a.klib").also { it.mkdirs() }
        val resultA = compileKlib(moduleA.sourceFile, dependency = null, outputFile = aKlib)
        assertEquals(ExitCode.OK, resultA.exitCode)

        val bKlib = tmpdir.resolve("b.klib").also { it.mkdirs() }
        val resultB = compileKlib(moduleB.sourceFile, dependency = aKlib, outputFile = bKlib)
        assertEquals(ExitCode.OK, resultB.exitCode)

        // remove transitive dependency `a`, to check that subsequent compilation of `c` would not fail,
        // since resolve on 1-st stage is performed without dependencies
        aKlib.deleteRecursively()
        val cKlib = tmpdir.resolve("c.klib").also { it.mkdirs() }
        val resultC = compileKlib(moduleC.sourceFile, dependency = bKlib, outputFile = cKlib)
        assertEquals(ExitCode.OK, resultC.exitCode)

        val resultJS = compileToJs(cKlib, dependency = bKlib, outputFile = cKlib)
        assertEquals(ExitCode.OK, resultJS.exitCode)
        assertTrue(resultJS.output.contains("warning: KLIB resolver: Could not find \"a\" in "))
        assertTrue(resultJS.output.contains("No function found for symbol 'a/a|a(kotlin.Int){}[0]'"))
    }

    private data class Module(val name: String, val dependencyNames: List<String>) {
        constructor(name: String, vararg dependencyNames: String) : this(name, dependencyNames.asList())

        lateinit var dependencies: List<Module>
        lateinit var sourceFile: File

        fun initDependencies(resolveDependency: (String) -> Module) {
            dependencies = dependencyNames.map(resolveDependency)
        }
    }

    private fun createKlibDir(tmpdir: File, name: String, version: Int): File =
        tmpdir.resolve("v$version").resolve(name).apply(File::mkdirs)

    // copy-pasted from KotlinStandardLibrariesPathProvider.kt
    private inline fun extractFromPropertyFirst(prop: String, onMissingProperty: () -> String): File {
        val path = System.getProperty(prop, null) ?: onMissingProperty()
        assert(File(path).exists()) { "$path not found" }
        return File(path)
    }

    // inspired by on `String.dist()` from KotlinStandardLibrariesPathProvider.kt
    private fun String.build() = "libraries/stdlib/build/libs/$this-${KotlinVersion.CURRENT}-SNAPSHOT.klib"
    private fun fullWasmJsStdlib() = extractFromPropertyFirst("kotlin.wasm.js.stdlib.klib.path") { "kotlin-stdlib-wasm-js".build() }

    private fun compileKlib(sourceFile: File, dependency: File?, outputFile: File): CompilationResult {
        val libraries = listOfNotNull(
            fullWasmJsStdlib(),
            dependency
        ).joinToString(File.pathSeparator) { it.absolutePath }

        val args = arrayOf(
            K2JSCompilerArguments::wasm.cliArgument,
            K2JSCompilerArguments::wasmTarget.cliArgument("wasm-js"),
            K2JSCompilerArguments::irProduceKlibDir.cliArgument,
            K2JSCompilerArguments::libraries.cliArgument, libraries,
            K2JSCompilerArguments::outputDir.cliArgument, outputFile.absolutePath,
            K2JSCompilerArguments::moduleName.cliArgument, outputFile.nameWithoutExtension,
            sourceFile.absolutePath
        )

        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            K2JSCompiler().execFullPathsInMessages(printStream, args)
        }

        return CompilationResult(exitCode, compilerXmlOutput.toString())
    }

    private fun compileToJs(entryModuleKlib: File, dependency: File?, outputFile: File): CompilationResult {
        val libraries = listOfNotNull(
            fullWasmJsStdlib(),
            dependency
        ).joinToString(File.pathSeparator) { it.absolutePath }

        val args = arrayOf(
            K2JSCompilerArguments::wasm.cliArgument,
            K2JSCompilerArguments::wasmTarget.cliArgument("wasm-js"),
            K2JSCompilerArguments::irProduceJs.cliArgument,
            K2JSCompilerArguments::includes.cliArgument(entryModuleKlib.absolutePath),
            K2JSCompilerArguments::libraries.cliArgument, libraries,
            K2JSCompilerArguments::outputDir.cliArgument, outputFile.absolutePath,
            K2JSCompilerArguments::moduleName.cliArgument, outputFile.nameWithoutExtension,
            K2JSCompilerArguments::target.cliArgument, "es2015",
        )

        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            K2JSCompiler().execFullPathsInMessages(printStream, args)
        }

        return CompilationResult(exitCode, compilerXmlOutput.toString())
    }

    private data class CompilationResult(val exitCode: ExitCode, val output: String) {
        fun assertSuccess() = JUnit4Assertions.assertTrue(exitCode == ExitCode.OK) {
            buildString {
                appendLine("Expected exit code: ${ExitCode.OK}, Actual: $exitCode")
                appendLine("Compiler output:")
                appendLine(output)
            }
        }

        fun assertFailure() = JUnit4Assertions.assertTrue(exitCode != ExitCode.OK) {
            buildString {
                appendLine("Expected exit code: any but ${ExitCode.OK}, Actual: $exitCode")
                appendLine("Compiler output:")
                appendLine(output)
            }
        }
    }

    private fun createModules(tmpdir: File, vararg modules: Module): List<Module> {
        val mapping: Map<String, Module> = modules.groupBy(Module::name).mapValues {
            it.value.singleOrNull() ?: error("Duplicated modules: ${it.value}")
        }

        modules.forEach { it.initDependencies(mapping::getValue) }

        val generatedSourcesDir = tmpdir.resolve("generated-sources")
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
}
