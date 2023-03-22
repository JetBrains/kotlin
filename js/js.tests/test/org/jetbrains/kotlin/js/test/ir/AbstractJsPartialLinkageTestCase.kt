/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.js.K2JsIrCompiler
import org.jetbrains.kotlin.cli.js.klib.*
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.js.testOld.V8IrJsTestChecker
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.Dependencies
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.Dependency
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.MAIN_MODULE_NAME
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.ModuleBuildDirs
import org.junit.jupiter.api.AfterEach
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.io.path.createTempDirectory

abstract class AbstractJsPartialLinkageNoICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.K1_NO_IC)
abstract class AbstractJsPartialLinkageNoICES6TestCase : AbstractJsPartialLinkageTestCase(CompilerType.K1_NO_IC_WITH_ES6)
abstract class AbstractJsPartialLinkageWithICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.K1_WITH_IC)
abstract class AbstractFirJsPartialLinkageNoICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.K2_NO_IC)

abstract class AbstractJsPartialLinkageTestCase(private val compilerType: CompilerType) {
    enum class CompilerType(val useFir: Boolean, val es6Mode: Boolean, val useIc: Boolean) {
        K1_NO_IC(useFir = false, es6Mode = false, useIc = false),
        K1_NO_IC_WITH_ES6(useFir = false, es6Mode = true, useIc = false),
        K1_WITH_IC(useFir = false, es6Mode = false, useIc = true),
        K2_NO_IC(useFir = true, es6Mode = false, useIc = false)
    }

    private val buildDir: File = createTempDirectory().toFile().also { it.mkdirs() }


    @AfterEach
    fun clearArtifacts() {
        buildDir.deleteRecursively()
    }

    private inner class JsTestConfiguration(testPath: String) : PartialLinkageTestUtils.TestConfiguration {
        override val testDir: File = File(testPath).absoluteFile
        override val buildDir: File get() = this@AbstractJsPartialLinkageTestCase.buildDir
        override val stdlibFile: File get() = File("libraries/stdlib/build/classes/kotlin/js/main").absoluteFile
        override val testModeConstructorParameters = mapOf("isJs" to "true")

        override fun customizeModuleSources(moduleName: String, moduleSourceDir: File) {
            if (moduleName == MAIN_MODULE_NAME)
                customizeMainModuleSources(moduleSourceDir)
        }

        override fun buildKlib(
            moduleName: String,
            buildDirs: ModuleBuildDirs,
            dependencies: Dependencies,
            klibFile: File,
        ) = this@AbstractJsPartialLinkageTestCase.buildKlib(moduleName, buildDirs, dependencies, klibFile)

        override fun buildBinaryAndRun(mainModule: Dependency, otherDependencies: Dependencies) =
            this@AbstractJsPartialLinkageTestCase.buildBinaryAndRun(mainModule, otherDependencies)

        override fun onNonEmptyBuildDirectory(directory: File) {
            directory.listFiles()?.forEach(File::deleteRecursively)
        }

        override fun onIgnoredTest() {
            /* Do nothing specific. JUnit 3 does not support programmatic tests muting. */
        }
    }

    // The entry point to generated test classes.
    fun runTest(@TestDataFile testPath: String) = PartialLinkageTestUtils.runTest(JsTestConfiguration(testPath))

    private fun customizeMainModuleSources(moduleSourceDir: File) {
        // Add the @JsExport annotation to make the box function visible to Node.
        moduleSourceDir.walkTopDown().forEach { file ->
            if (file.extension == "kt") {
                var modified = false
                val lines = file.readLines().map { line ->
                    if (line.startsWith("fun $BOX_FUN_FQN()")) {
                        modified = true
                        "@OptIn(ExperimentalJsExport::class) @JsExport $line"
                    } else
                        line
                }
                if (modified) file.writeText(lines.joinToString("\n"))
            }
        }
    }

    fun buildKlib(moduleName: String, buildDirs: ModuleBuildDirs, dependencies: Dependencies, klibFile: File) {
        val kotlinSourceFilePaths = mutableListOf<String>()

        buildDirs.sourceDir.walkTopDown().forEach { sourceFile ->
            if (sourceFile.isFile) when (sourceFile.extension) {
                "kt" -> kotlinSourceFilePaths += sourceFile.absolutePath
                "js" -> {
                    // This is needed to preserve *.js files from test data which are required for tests with `external` declarations:
                    sourceFile.copyTo(buildDirs.outputDir.resolve(sourceFile.relativeTo(buildDirs.sourceDir)), overwrite = true)
                }
            }
        }

        // Build KLIB:
        runCompilerViaCLI(
            listOf(
                "-Xir-produce-klib-file",
                "-ir-output-dir", klibFile.parentFile.absolutePath,
                "-ir-output-name", moduleName,
                "-Werror" // Halt on any unexpected warning.
            ),
            dependencies.toCompilerArgs(),
            listOf(
                "-language-version", "2.0",
                "-Xsuppress-version-warnings" // Don't fail on language version warnings.
            ).takeIf { compilerType.useFir },
            kotlinSourceFilePaths
        )
    }

    private fun buildBinaryAndRun(mainModule: Dependency, otherDependencies: Dependencies) {
        // The modules in `Dependencies.regularDependencies` are already in topological order.
        // It is important to pass the provided and the produced JS files to Node in exactly the same order.
        val knownModulesInTopologicalOrder: List<ModuleDetails> = buildList {
            otherDependencies.regularDependencies.mapTo(this, ::ModuleDetails)
            this += ModuleDetails(mainModule)
        }
        val knownModuleNames: Set<ModuleName> = knownModulesInTopologicalOrder.mapTo(hashSetOf(), ModuleDetails::name)

        val binariesDir: File = File(buildDir, BIN_DIR_NAME).also { it.mkdirs() }

        runCompilerViaCLI(
            listOf(
                "-Xir-produce-js",
                "-Xir-per-module",
                "-module-kind", "plain",
                "-Xinclude=${mainModule.libraryFile.absolutePath}",
                "-ir-output-dir", binariesDir.absolutePath,
                "-ir-output-name", MAIN_MODULE_NAME,
                // IMPORTANT: Omitting PL arguments here. The default PL mode should be in effect.
                // "-Xpartial-linkage=enable", "-Xpartial-linkage-loglevel=INFO",
                "-Werror"
            ),
            listOf(
                "-Xcache-directory",
                buildDir.resolve("libs-cache").absolutePath
            ).takeIf { compilerType.useIc },
            otherDependencies.toCompilerArgs(),
            listOf("-Xes-classes").takeIf { compilerType.es6Mode }
        )

        // All JS files produced during the compiler call.
        val producedBinaries: Map<ModuleName, File> = binariesDir.walkTopDown()
            .filter { binaryFile -> binaryFile.extension == "js" }
            .associateBy { binaryFile -> binaryFile.guessModuleName() }

        val unexpectedModuleNames = producedBinaries.keys - knownModuleNames
        check(unexpectedModuleNames.isEmpty()) { "Unexpected module names: $unexpectedModuleNames" }

        val allBinaries: List<File> = buildList {
            knownModulesInTopologicalOrder.forEach { moduleDetails ->
                // A set of JS files directly out of test data (aka "external" JS files) to be used immediately in the application:
                val providedBinaries: List<File> = moduleDetails.outputDir.listFiles()?.filter { it.extension == "js" }.orEmpty()
                addAll(providedBinaries)

                // A JS file produced by the compiler for the given module:
                val producedBinary: File? = producedBinaries[moduleDetails.name]
                producedBinary?.let(::add)
            }
        }

        executeAndCheckBinaries(MAIN_MODULE_NAME, allBinaries)
    }

    private fun File.guessModuleName(): String {
        return when {
            extension != "js" -> error("Not a JS file: $this")
            name.startsWith("kotlin-") && "stdlib" in name -> "stdlib"
            else -> nameWithoutExtension.removePrefix("kotlin_")
        }
    }

    private fun Dependencies.toCompilerArgs(): List<String> = buildList {
        if (regularDependencies.isNotEmpty()) {
            this += "-libraries"
            this += regularDependencies.joinToString(File.pathSeparator) { it.libraryFile.absolutePath }
        }
        if (friendDependencies.isNotEmpty()) {
            this += "-Xfriend-modules=${friendDependencies.joinToString(File.pathSeparator) { it.libraryFile.absolutePath }}"
        }
    }

    private fun runCompilerViaCLI(vararg compilerArgs: List<String?>?) {
        val allCompilerArgs = compilerArgs.flatMap { args -> args.orEmpty().filterNotNull() }.toTypedArray()

        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            K2JsIrCompiler().execFullPathsInMessages(printStream, allCompilerArgs)
        }

        if (exitCode != ExitCode.OK)
            throw AssertionError(
                buildString {
                    appendLine("Compiler failure.")
                    appendLine("Exit code = $exitCode.")
                    appendLine("Compiler messages:")
                    appendLine("==========")
                    appendLine(compilerXmlOutput.toString(Charsets.UTF_8.name()))
                    appendLine("==========")
                }
            )
    }

    private fun executeAndCheckBinaries(mainModuleName: String, dependencies: Collection<File>) {
        val checker = V8IrJsTestChecker

        val filePaths = dependencies.map { it.canonicalPath }
        checker.check(filePaths, mainModuleName, null, BOX_FUN_FQN, "OK", withModuleSystem = false)
    }

    companion object {
        private const val BIN_DIR_NAME = "_bins_js"
        private const val BOX_FUN_FQN = "box"
    }
}

private typealias ModuleName = String
private class ModuleDetails(val name: ModuleName, val outputDir: File) {
    constructor(dependency: Dependency) : this(dependency.moduleName, dependency.libraryFile.parentFile)
}
