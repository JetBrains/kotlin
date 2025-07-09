/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.js.test.ir.AbstractJsCompilerInvocationTest.CompilerType
import org.jetbrains.kotlin.js.test.klib.customJsCompilerSettings
import org.jetbrains.kotlin.js.testOld.V8JsTestChecker
import org.jetbrains.kotlin.klib.KlibCompilerEdition
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.Dependencies
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.Dependency
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.MAIN_MODULE_NAME
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import org.junit.jupiter.api.AfterEach
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.io.path.createTempDirectory

/**
 * This is a base class for tests with repetitive Kotlin/JS compiler invocations.
 *
 * Examples:
 * - Partial linkage tests:
 *   - Build multiple KLIBs with dependencies between some of them
 *   - Rebuild and substitute some of those KLIBs
 *   - Finally, build a binary and run it
 * - KLIB compatibility tests:
 *   - Build KLIBs with one [KlibCompilerEdition]
 *   - Then build a binary with another [KlibCompilerEdition]
 */
abstract class AbstractJsCompilerInvocationTest(protected val compilerType: CompilerType) {
    enum class CompilerType(val es6Mode: Boolean, val useIc: Boolean) {
        NO_IC(es6Mode = false, useIc = false),
        NO_IC_WITH_ES6(es6Mode = true, useIc = false),
        WITH_IC(es6Mode = false, useIc = true),
    }

    protected val buildDir: File = createTempDirectory().toRealPath().toFile().also { it.mkdirs() }

    @AfterEach
    fun clearArtifacts() {
        buildDir.deleteRecursively()
    }
}

private typealias ModuleName = String

private class ModuleDetails(val name: ModuleName, val outputDir: File) {
    constructor(dependency: Dependency) : this(dependency.moduleName, dependency.libraryFile.parentFile)
}

private fun customCompilerCall(): (PrintStream, Array<String>) -> ExitCode = { printStream: PrintStream, args: Array<String> ->
    customJsCompilerSettings.customCompiler.callCompiler(printStream, *args)
}

private fun currentCompilerCall() = { printStream: PrintStream, args: Array<String> ->
    K2JSCompiler().execFullPathsInMessages(printStream, args)
}

internal class JsCompilerInvocationTestConfiguration(
    override val buildDir: File,
    val compilerType: CompilerType,
) : KlibCompilerInvocationTestUtils.TestConfiguration {
    override val stdlibFile: File get() = File("libraries/stdlib/build/classes/kotlin/js/main").absoluteFile
    override val targetBackend get() = if (compilerType.es6Mode) TargetBackend.JS_IR_ES6 else TargetBackend.JS_IR

    override fun onIgnoredTest() {
        /* Do nothing specific. JUnit 3 does not support programmatic tests muting. */
    }
}

internal class JsCompilerInvocationTestBinaryArtifact(
    val mainModuleName: String,
    val boxFunctionFqName: String,
    val jsFiles: List<File>,
) : KlibCompilerInvocationTestUtils.BinaryArtifact

internal class JsCompilerInvocationTestArtifactBuilder(
    private val configuration: JsCompilerInvocationTestConfiguration,
) : KlibCompilerInvocationTestUtils.ArtifactBuilder<JsCompilerInvocationTestBinaryArtifact> {
    override fun buildKlib(
        module: KlibCompilerInvocationTestUtils.TestStructure.ModuleUnderTest,
        dependencies: Dependencies,
        compilerEdition: KlibCompilerEdition,
        compilerArguments: List<String>,
    ) {
        val kotlinSourceFilePaths = composeSourceFile(module.sourceDir, module.outputDir)
        val preprocessedDependencies = dependencies.replaceStdlib(compilerEdition)

        // Build KLIB:
        runCompilerViaCLI(
            compilerEdition,
            listOf(
                K2JSCompilerArguments::irProduceKlibFile.cliArgument,
                K2JSCompilerArguments::outputDir.cliArgument, module.klibFile.parentFile.absolutePath,
                K2JSCompilerArguments::moduleName.cliArgument, module.moduleInfo.moduleName,
            ),
            preprocessedDependencies.toCompilerArgs(),
            compilerArguments,
            kotlinSourceFilePaths
        )
    }

    private fun composeSourceFile(moduleSourceDir: File, moduleOutputDir: File): MutableList<String> {
        val kotlinSourceFilePaths = mutableListOf<String>()
        moduleSourceDir.walkTopDown().forEach { sourceFile ->
            if (sourceFile.isFile) when (sourceFile.extension) {
                "kt" -> kotlinSourceFilePaths += sourceFile.absolutePath
                "js" -> {
                    // This is needed to preserve *.js files from test data which are required for tests with `external` declarations:
                    sourceFile.copyTo(moduleOutputDir.resolve(sourceFile.relativeTo(moduleSourceDir)), overwrite = true)
                }
            }
        }
        return kotlinSourceFilePaths
    }

    override fun buildBinary(
        mainModule: Dependency,
        otherDependencies: Dependencies,
        compilerEdition: KlibCompilerEdition,
    ): JsCompilerInvocationTestBinaryArtifact {
        val preprocessedDependencies = otherDependencies.replaceStdlib(compilerEdition)

        // The modules in `Dependencies.regularDependencies` are already in topological order.
        // It is important to pass the provided and the produced JS files to Node in exactly the same order.
        val knownModulesInTopologicalOrder: List<ModuleDetails> = buildList {
            preprocessedDependencies.regularDependencies.mapTo(this, ::ModuleDetails)
            this += ModuleDetails(mainModule)
        }
        val knownModuleNames: Set<ModuleName> = knownModulesInTopologicalOrder.mapTo(hashSetOf(), ModuleDetails::name)

        val binariesDir: File = File(configuration.buildDir, BIN_DIR_NAME).also { it.mkdirs() }

        runCompilerViaCLI(
            compilerEdition,
            listOf(
                K2JSCompilerArguments::irProduceJs.cliArgument,
                K2JSCompilerArguments::irPerModule.cliArgument,
                K2JSCompilerArguments::moduleKind.cliArgument, "plain",
                K2JSCompilerArguments::includes.cliArgument(mainModule.libraryFile.absolutePath),
                K2JSCompilerArguments::outputDir.cliArgument, binariesDir.absolutePath,
                K2JSCompilerArguments::moduleName.cliArgument, MAIN_MODULE_NAME,
            ),
            listOf(
                K2JSCompilerArguments::cacheDirectory.cliArgument,
                configuration.buildDir.resolve("libs-cache").absolutePath
            ).takeIf { configuration.compilerType.useIc },
            preprocessedDependencies.toCompilerArgs(),
            listOf(K2JSCompilerArguments::useEsClasses.cliArgument).takeIf { configuration.compilerType.es6Mode }
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

        return JsCompilerInvocationTestBinaryArtifact(
            mainModuleName = MAIN_MODULE_NAME,
            boxFunctionFqName = BOX_FUN_FQN,
            jsFiles = allBinaries,
        )
    }

    private fun Dependencies.replaceStdlib(compilerEdition: KlibCompilerEdition): Dependencies =
        if (compilerEdition == KlibCompilerEdition.CURRENT)
            this
        else Dependencies(
            regularDependencies = regularDependencies.replaceStdLib(),
            friendDependencies = friendDependencies
        )

    private fun Set<Dependency>.replaceStdLib(): Set<Dependency> = mapToSetOrEmpty {
        if (it.moduleName == "stdlib") Dependency("stdlib", customJsCompilerSettings.stdlib) else it
    }.toSet()

    private fun Dependencies.toCompilerArgs(): List<String> = buildList {
        if (regularDependencies.isNotEmpty()) {
            this += K2JSCompilerArguments::libraries.cliArgument
            this += regularDependencies.joinToString(File.pathSeparator) { it.libraryFile.absolutePath }
        }
        if (friendDependencies.isNotEmpty()) {
            this += K2JSCompilerArguments::friendModules.cliArgument(friendDependencies.joinToString(File.pathSeparator) { it.libraryFile.absolutePath })
        }
    }

    private fun runCompilerViaCLI(compilerEdition: KlibCompilerEdition, vararg compilerArgs: List<String?>?) {
        val allCompilerArgs = compilerArgs.flatMap { args -> args.orEmpty().filterNotNull() }.toTypedArray()

        val invokeCompiler = when (compilerEdition) {
            KlibCompilerEdition.CURRENT -> currentCompilerCall()
            KlibCompilerEdition.CUSTOM -> customCompilerCall()
        }

        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            invokeCompiler(printStream, allCompilerArgs)
        }

        if (exitCode != ExitCode.OK)
            throw AssertionError(
                buildString {
                    appendLine("Compiler ($compilerEdition) failure.")
                    appendLine("Exit code = $exitCode.")
                    appendLine("Compiler arguments: ${allCompilerArgs.joinToString(separator = ", ")}")
                    appendLine("Compiler messages:")
                    appendLine("==========")
                    appendLine(compilerXmlOutput.toString(Charsets.UTF_8.name()))
                    appendLine("==========")
                }
            )
    }

    private fun File.guessModuleName(): String {
        return when {
            extension != "js" -> error("Not a JS file: $this")
            name.startsWith("kotlin-") && "stdlib" in name -> "stdlib"
            else -> nameWithoutExtension.removePrefix("kotlin_")
        }
    }

    companion object {
        private const val BIN_DIR_NAME = "_bins_js"
        private const val BOX_FUN_FQN = "box"
    }
}

internal object JsCompilerInvocationTestBinaryRunner :
    KlibCompilerInvocationTestUtils.BinaryRunner<JsCompilerInvocationTestBinaryArtifact> {

    override fun runBinary(binaryArtifact: JsCompilerInvocationTestBinaryArtifact) {
        val filePaths = binaryArtifact.jsFiles.map { it.canonicalPath }
        V8JsTestChecker.check(
            filePaths, binaryArtifact.mainModuleName, null,
            binaryArtifact.boxFunctionFqName, "OK", withModuleSystem = false,
        )
    }
}
