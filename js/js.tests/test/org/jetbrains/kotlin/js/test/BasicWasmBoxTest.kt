/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.backend.wasm.compileWasm
import org.jetbrains.kotlin.backend.wasm.wasmPhases
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.loadKlib
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.js.test.engines.SpiderMonkeyEngine
import org.jetbrains.kotlin.library.resolver.impl.KotlinLibraryResolverResultImpl
import org.jetbrains.kotlin.library.resolver.impl.KotlinResolvedLibraryImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.Directives
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TestFiles
import java.io.Closeable
import java.io.File
import java.lang.Boolean.getBoolean

private val wasmRuntimeKlib =
    loadKlib("libraries/stdlib/js-ir/build/wasmRuntime/klib")

abstract class BasicWasmBoxTest(
    private val pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    pathToRootOutputDir: String = TEST_DATA_DIR_PATH
) : KotlinTestWithEnvironment() {
    private val testGroupOutputDirForCompilation = File(pathToRootOutputDir + "out/" + testGroupOutputDirPrefix)

    private val spiderMonkey by lazy { SpiderMonkeyEngine() }

    fun doTest(filePath: String) {
        val file = File(filePath)
        val outputDir = getOutputDir(file)
        val fileContent = KotlinTestUtils.doLoadFile(file)

        TestFileFactoryImpl().use { testFactory ->
            val inputFiles: MutableList<TestFile> = TestFiles.createTestFiles(file.name, fileContent, testFactory, true, "")
            val testPackage = testFactory.testPackage
            val outputFileBase = outputDir.absolutePath + "/" + getTestName(true)
            val outputWatFile = outputFileBase + ".wat"
            val outputJsFile = outputFileBase + ".js"

            val kotlinFiles = inputFiles.filter { it.fileName.endsWith(".kt") }
            val psiFiles = createPsiFiles(kotlinFiles.map { File(it.fileName).canonicalPath }.sorted())
            val config = createConfig()
            translateFiles(
                psiFiles.map(TranslationUnit::SourceFile),
                File(outputWatFile),
                File(outputJsFile),
                config,
                testPackage,
                TEST_FUNCTION
            )

            spiderMonkey.runFile(outputJsFile)
        }
    }

    private fun getOutputDir(file: File, testGroupOutputDir: File = testGroupOutputDirForCompilation): File {
        val stopFile = File(pathToTestDir)
        return generateSequence(file.parentFile) { it.parentFile }
            .takeWhile { it != stopFile }
            .map { it.name }
            .toList().asReversed()
            .fold(testGroupOutputDir, ::File)
    }

    private fun translateFiles(
        units: List<TranslationUnit>,
        outputWatFile: File,
        outputJsFile: File,
        config: JsConfig,
        testPackage: String?,
        testFunction: String
    ) {
        val filesToCompile = units.map { (it as TranslationUnit.SourceFile).file }
        val debugMode = getBoolean("kotlin.js.debugMode")

        val phaseConfig = if (debugMode) {
            val allPhasesSet = wasmPhases.toPhaseMap().values.toSet()
            val dumpOutputDir = File(outputWatFile.parent, outputWatFile.nameWithoutExtension + "-irdump")
            println("\n ------ Dumping phases to file://$dumpOutputDir")
            PhaseConfig(
                wasmPhases,
                dumpToDirectory = dumpOutputDir.path,
                toDumpStateAfter = allPhasesSet,
                toValidateStateAfter = allPhasesSet,
                dumpOnlyFqName = null
            )
        } else {
            PhaseConfig(wasmPhases)
        }

        val compilerResult = compileWasm(
            project = config.project,
            files = filesToCompile,
            analyzer = AnalyzerWithCompilerReport(config.configuration),
            configuration = config.configuration,
            phaseConfig = phaseConfig,
            // TODO: Bypass the resolver fow wasm.
            allDependencies = KotlinLibraryResolverResultImpl(listOf(KotlinResolvedLibraryImpl(wasmRuntimeKlib))),
            friendDependencies = emptyList(),
            exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, testFunction)))
        )

        outputWatFile.write(compilerResult.wat)

        val runtime = File("libraries/stdlib/wasm/runtime/runtime.js").readText()

        val testRunner = """
            const wat = read(String.raw`${outputWatFile.absoluteFile}`);
            const wasmBinary = wasmTextToBinary(wat);
            const wasmModule = new WebAssembly.Module(wasmBinary);
            const wasmInstance = new WebAssembly.Instance(wasmModule, { runtime });

            const actualResult = wasmInstance.exports.$testFunction();
            if (actualResult !== "OK")
                throw `Wrong box result '${'$'}{actualResult}'; Expected "OK"`;
        """.trimIndent()

        outputJsFile.write(runtime + "\n" + compilerResult.js + "\n" + testRunner)
    }

    private fun createPsiFile(fileName: String): KtFile {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")

        return psiManager.findFile(file) as KtFile
    }

    private fun createPsiFiles(fileNames: List<String>): List<KtFile> = fileNames.map(this::createPsiFile)

    private fun createConfig(): JsConfig {
        val configuration = environment.configuration.copy()
        configuration.put(CommonConfigurationKeys.MODULE_NAME, TEST_MODULE)
        return JsConfig(project, configuration, null, null)
    }

    private inner class TestFileFactoryImpl : TestFiles.TestFileFactoryNoModules<TestFile>(), Closeable {
        override fun create(fileName: String, text: String, directives: Directives): TestFile {
            val ktFile = KtPsiFactory(project).createFile(text)
            val boxFunction = ktFile.declarations.find { it is KtNamedFunction && it.name == TEST_FUNCTION }
            if (boxFunction != null) {
                testPackage = ktFile.packageFqName.asString()
                if (testPackage?.isEmpty() == true) {
                    testPackage = null
                }
            }

            val temporaryFile = File(tmpDir, "WASM_TEST/$fileName")
            KotlinTestUtils.mkdirs(temporaryFile.parentFile)
            temporaryFile.writeText(text, Charsets.UTF_8)

            return TestFile(temporaryFile.absolutePath)
        }

        var testPackage: String? = null
        val tmpDir = KotlinTestUtils.tmpDir("wasm-tests")

        override fun close() {
            FileUtil.delete(tmpDir)
        }
    }

    private class TestFile(val fileName: String)

    override fun createEnvironment() =
        KotlinCoreEnvironment.createForTests(testRootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)

    companion object {
        const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"
        const val TEST_MODULE = "WASM_TESTS"
        private const val TEST_FUNCTION = "box"
    }
}

private fun File.write(text: String) {
    parentFile.mkdirs()
    writeText(text)
}
