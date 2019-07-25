/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.wasm.semantics

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.loadKlib
import org.jetbrains.kotlin.ir.backend.js.wasm.compileWasm
import org.jetbrains.kotlin.ir.backend.js.wasm.wasmPhases
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.js.test.NashornJsTestChecker
import org.jetbrains.kotlin.js.test.V8JsTestChecker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TargetBackend
import java.io.Closeable
import java.io.File
import java.lang.Boolean.getBoolean

private val wasmRuntimeKlib =
    loadKlib("compiler/ir/serialization.js/build/wasmRuntime/klib")

open class BasicWasmBoxTest(
    private val pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    pathToRootOutputDir: String = TEST_DATA_DIR_PATH
) : KotlinTestWithEnvironment() {
    private val testGroupOutputDirForCompilation = File(pathToRootOutputDir + "out/" + testGroupOutputDirPrefix)

    private val testChecker get() = if (runTestInNashorn) NashornJsTestChecker else V8JsTestChecker

    fun doTest(filePath: String) {
        val file = File(filePath)
        val outputDir = getOutputDir(file)
        val fileContent = KotlinTestUtils.doLoadFile(file)

        TestFileFactoryImpl().use { testFactory ->
            val inputFiles: MutableList<TestFile> = KotlinTestUtils.createTestFiles(file.name, fileContent, testFactory, true, "")
            val testPackage = testFactory.testPackage
            val mainModuleName = TEST_MODULE
            val outputFileName = outputDir.absolutePath + "/" + getTestName(true)
            val kotlinFiles = inputFiles.filter { it.fileName.endsWith(".kt") }
            val psiFiles = createPsiFiles(kotlinFiles.map { File(it.fileName).canonicalPath }.sorted())
            val config = createConfig()
            val outputFile = File(outputFileName)
            translateFiles(
                psiFiles.map(TranslationUnit::SourceFile), outputFile, config,
                testPackage, TEST_FUNCTION
            )

            val dontRunGeneratedCode = InTextDirectivesUtils.dontRunGeneratedCode(TargetBackend.JS, file)
            if (!dontRunGeneratedCode) {
                testChecker.check(listOf(outputFileName), mainModuleName, testPackage, TEST_FUNCTION, "OK", false)
            }
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
        outputFile: File,
        config: JsConfig,
        testPackage: String?,
        testFunction: String
    ) {
        val filesToCompile = units
            .map { (it as TranslationUnit.SourceFile).file }

        val runtimeKlibs = listOf(wasmRuntimeKlib)

        val debugMode = false

        val phaseConfig = if (debugMode) {
            val allPhasesSet = wasmPhases.toPhaseMap().values.toSet()
            val dumpOutputDir = File(outputFile.parent, outputFile.nameWithoutExtension + "-irdump")
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

        val jsCode = compileWasm(
            project = config.project,
            files = filesToCompile,
            configuration = config.configuration,
            phaseConfig = phaseConfig,
            allDependencies = runtimeKlibs,
            friendDependencies = emptyList(),
            exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, testFunction)))
        )

        outputFile.write(jsCode)
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

    private inner class TestFileFactoryImpl : KotlinTestUtils.TestFileFactoryNoModules<TestFile>(), Closeable {
        override fun create(fileName: String, text: String, directives: MutableMap<String, String>): TestFile {
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

        @JvmStatic
        private val runTestInNashorn = getBoolean("kotlin.js.useNashorn")

        const val TEST_MODULE = "WASM_TESTS"
        private const val TEST_FUNCTION = "box"
    }
}

private fun File.write(text: String) {
    parentFile.mkdirs()
    writeText(text)
}
