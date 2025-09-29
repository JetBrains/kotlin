/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.klib.KlibCompilerChangeScenario
import org.jetbrains.kotlin.klib.KlibCompilerEdition
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.Dependencies
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.Dependency
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.MAIN_MODULE_NAME
import org.jetbrains.kotlin.klib.PartialLinkageTestStructureExtractor
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator
import org.jetbrains.kotlin.wasm.test.AbstractWasmPartialLinkageTestCase.CompilerType
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.io.path.createTempDirectory

abstract class AbstractWasmPartialLinkageNoICTestCase : AbstractWasmPartialLinkageTestCase(CompilerType.NO_IC)
abstract class AbstractWasmPartialLinkageWithICTestCase : AbstractWasmPartialLinkageTestCase(CompilerType.WITH_IC)

abstract class AbstractWasmPartialLinkageTestCase(private val compilerType: CompilerType) {
    enum class CompilerType(val useIc: Boolean) {
        NO_IC(useIc = false),
        WITH_IC(useIc = true),
    }

    private val buildDir: File = createTempDirectory().toRealPath().toFile().also { it.mkdirs() }

    @AfterEach
    fun clearArtifacts() {
        buildDir.deleteRecursively()
    }

    // The entry point to generated test classes.
    fun runTest(@TestDataFile testDir: String) {
        val configuration = WasmCompilerInvocationTestConfiguration(
            buildDir = buildDir,
            compilerType = compilerType,
        )

        KlibCompilerInvocationTestUtils.runTest(
            testStructure = WasmPartialLinkageTestStructureExtractor(buildDir).extractTestStructure(File(testDir).absoluteFile),
            testConfiguration = configuration,
            artifactBuilder = WasmCompilerInvocationTestArtifactBuilder(configuration),
            binaryRunner = WasmCompilerInvocationTestBinaryRunner,
            compilerEditionChange = KlibCompilerChangeScenario.NoChange,
        )
    }
}

internal class WasmCompilerInvocationTestConfiguration(
    override val buildDir: File,
    val compilerType: CompilerType,
) : KlibCompilerInvocationTestUtils.TestConfiguration {
    override val stdlibFile: File get() = File(WasmEnvironmentConfigurator.stdlibPath(WasmTarget.JS)).absoluteFile
    override val targetBackend get() = TargetBackend.WASM


    override fun onIgnoredTest() {
        Assumptions.abort<Unit>()
    }
}

internal class WasmPartialLinkageTestStructureExtractor(
    override val buildDir: File,
) : PartialLinkageTestStructureExtractor() {
    override val testModeConstructorParameters = mapOf("isWasm" to "true")

    override fun customizeModuleSources(moduleName: String, moduleSourceDir: File) {
        if (moduleName == KlibCompilerInvocationTestUtils.MAIN_MODULE_NAME) {
            File(moduleSourceDir, "runner.kt")
                .writeText("@kotlin.wasm.WasmExport fun runBoxTest() = println($BOX_FUN_FQN())")
        }
    }

    companion object {
        private const val BOX_FUN_FQN = "box"
    }
}

internal class WasmCompilerInvocationTestBinaryArtifact(
    val jsFiles: List<File>,
    val binariesDir: File,
    val runnerFileName: String,
) : KlibCompilerInvocationTestUtils.BinaryArtifact

internal class WasmCompilerInvocationTestArtifactBuilder(
    private val configuration: WasmCompilerInvocationTestConfiguration,
) : KlibCompilerInvocationTestUtils.ArtifactBuilder<WasmCompilerInvocationTestBinaryArtifact> {
    override fun buildKlib(
        module: KlibCompilerInvocationTestUtils.TestStructure.ModuleUnderTest,
        dependencies: Dependencies,
        compilerEdition: KlibCompilerEdition,
        compilerArguments: List<String>,
    ) {
        require(compilerEdition == KlibCompilerEdition.CURRENT) { "Partial Linkage tests accept only Current compiler" }

        val kotlinSourceFilePaths = mutableListOf<String>()

        module.sourceDir.walkTopDown().forEach { sourceFile ->
            if (sourceFile.isFile) when (sourceFile.extension) {
                "kt" -> kotlinSourceFilePaths += sourceFile.absolutePath
                "js" -> {
                    // This is needed to preserve *.js files from test data which are required for tests with `external` declarations:
                    sourceFile.copyTo(module.outputDir.resolve(sourceFile.relativeTo(module.sourceDir)), overwrite = true)
                }
            }
        }

        // Build KLIB:
        runCompilerViaCLI(
            listOf(
                K2JSCompilerArguments::irProduceKlibFile.cliArgument,
                K2JSCompilerArguments::outputDir.cliArgument, module.klibFile.parentFile.absolutePath,
                K2JSCompilerArguments::moduleName.cliArgument, module.moduleInfo.moduleName,
                K2JSCompilerArguments::wasm.cliArgument
            ),
            dependencies.toCompilerArgs(),
            compilerArguments,
            kotlinSourceFilePaths
        )
    }

    override fun buildBinary(
        mainModule: Dependency,
        otherDependencies: Dependencies,
        compilerEdition: KlibCompilerEdition,
    ): WasmCompilerInvocationTestBinaryArtifact {
        require(compilerEdition == KlibCompilerEdition.CURRENT) { "Partial Linkage tests accept only Current compiler" }

        val binariesDir: File = File(configuration.buildDir, BIN_DIR_NAME).also { it.mkdirs() }

        runCompilerViaCLI(
            listOf(
                K2JSCompilerArguments::irProduceJs.cliArgument,
                K2JSCompilerArguments::irPerModule.cliArgument,
                K2JSCompilerArguments::moduleKind.cliArgument, "plain",
                K2JSCompilerArguments::includes.cliArgument(mainModule.libraryFile.absolutePath),
                K2JSCompilerArguments::outputDir.cliArgument, binariesDir.absolutePath,
                K2JSCompilerArguments::moduleName.cliArgument, MAIN_MODULE_NAME,
                K2JSCompilerArguments::wasm.cliArgument,
            ),
            listOf(
                K2JSCompilerArguments::cacheDirectory.cliArgument(configuration.buildDir.resolve("libs-cache").absolutePath),
            ).takeIf { configuration.compilerType.useIc },
            otherDependencies.toCompilerArgs(),
        )

        val mainModuleMjsName = "$MAIN_MODULE_NAME.mjs"
        val resultMjs = File(binariesDir, mainModuleMjsName)
        if (!resultMjs.exists()) error("Produced binary not found")

        val runnerFileName = "runner.mjs"
        File(binariesDir, runnerFileName).writeText(
            """
            const { runBoxTest } = await import('./$mainModuleMjsName');
            runBoxTest();
            """,
        )

        val additionalJsFiles = mutableListOf<File>()
        additionalJsFiles.addAll(getAdditionalJsFiles(mainModule))
        otherDependencies.regularDependencies.flatMapTo(additionalJsFiles) { getAdditionalJsFiles(it) }
        otherDependencies.friendDependencies.flatMapTo(additionalJsFiles) { getAdditionalJsFiles(it) }

        return WasmCompilerInvocationTestBinaryArtifact(
            jsFiles = additionalJsFiles,
            binariesDir = binariesDir,
            runnerFileName = runnerFileName,
        )
    }

    private fun Dependencies.toCompilerArgs(): List<String> = buildList {
        if (regularDependencies.isNotEmpty()) {
            this += K2JSCompilerArguments::libraries.cliArgument
            this += regularDependencies.joinToString(File.pathSeparator) { it.libraryFile.absolutePath }
        }
        if (friendDependencies.isNotEmpty()) {
            this += K2JSCompilerArguments::friendModules.cliArgument(friendDependencies.joinToString(File.pathSeparator) { it.libraryFile.absolutePath })
        }
    }

    private fun getAdditionalJsFiles(dependency: Dependency): List<File> {
        val outputDirectory = dependency.libraryFile.let {
            it.takeIf { it.isDirectory } ?: it.parentFile
        }
        return outputDirectory.listFiles()!!.filter { it.extension == "js" }
    }

    private fun runCompilerViaCLI(vararg compilerArgs: List<String?>?) {
        val allCompilerArgs = compilerArgs.flatMap { args -> args.orEmpty().filterNotNull() }.toTypedArray()

        val compilerXmlOutput = ByteArrayOutputStream()
        val exitCode = PrintStream(compilerXmlOutput).use { printStream ->
            K2JSCompiler().execFullPathsInMessages(printStream, allCompilerArgs)
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

    companion object {
        private const val BIN_DIR_NAME = "_bins_wasm"
    }
}

internal object WasmCompilerInvocationTestBinaryRunner :
    KlibCompilerInvocationTestUtils.BinaryRunner<WasmCompilerInvocationTestBinaryArtifact> {

    override fun runBinary(binaryArtifact: WasmCompilerInvocationTestBinaryArtifact) {
        val result = WasmVM.V8.run(
            entryFile = binaryArtifact.runnerFileName,
            jsFiles = binaryArtifact.jsFiles.map { it.absolutePath },
            workingDirectory = binaryArtifact.binariesDir
        )
        check("OK" == result.trim())
    }
}
