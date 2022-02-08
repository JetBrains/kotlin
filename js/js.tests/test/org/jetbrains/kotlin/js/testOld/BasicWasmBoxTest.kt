/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.compileWasm
import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.backend.wasm.wasmPhases
import org.jetbrains.kotlin.checkers.parseLanguageVersionSettings
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.prepareAnalyzedSourceModule
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.js.testOld.engines.ExternalTool
import org.jetbrains.kotlin.js.testOld.engines.SpiderMonkeyEngine
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.Closeable
import java.io.File

abstract class BasicWasmBoxTest(
    private val pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    pathToRootOutputDir: String = TEST_DATA_DIR_PATH
) : KotlinTestWithEnvironment() {
    private val testGroupOutputDirForCompilation = File(pathToRootOutputDir + "out/" + testGroupOutputDirPrefix)

    private val spiderMonkey by lazy { SpiderMonkeyEngine() }

    private val COMMON_FILES_NAME = "_common"

    @Suppress("UNUSED_PARAMETER")
    fun doTestWithCoroutinesPackageReplacement(filePath: String, coroutinesPackage: String) {
        TODO("TestWithCoroutinesPackageReplacement are not supported")
    }

    fun doTest(filePath: String) = doTestWithTransformer(filePath) { it }
    fun doTestWithTransformer(filePath: String, transformer: java.util.function.Function<String, String>) {
        val file = File(filePath)

        val outputDir = getOutputDir(file)
        val fileContent = transformer.apply(KtTestUtil.doLoadFile(file))

        TestFileFactoryImpl().use { testFactory ->
            val inputFiles: MutableList<TestFile> = TestFiles.createTestFiles(file.name, fileContent, testFactory, true)
            val testPackage = testFactory.testPackage
            val outputFileBase = outputDir.absolutePath + "/" + getTestName(true)
            val outputWatFile = File("$outputFileBase.wat")
            val outputWasmFile = File("$outputFileBase.wasm")
            val outputJsFile = File("$outputFileBase.js")
            val outputBrowserDir = File("$outputFileBase.browser")
            val outputFileNoDceBase = "${outputFileBase}NoDce"
            val outputWatNoDceFile = File("$outputFileNoDceBase.wat")
            val outputWasmNoDceFile = File("$outputFileNoDceBase.wasm")
            val outputJsNoDceFile = File("$outputFileNoDceBase.js")
            val outputBrowserNoDceDir = File("$outputFileNoDceBase.browser")
            val languageVersionSettings = inputFiles.firstNotNullOfOrNull { it.languageVersionSettings }

            val kotlinFiles = mutableListOf<String>()
            val jsFilesBefore = mutableListOf<String>()
            val jsFilesAfter = mutableListOf<String>()

            inputFiles.forEach {
                val name = it.fileName
                when {
                    name.endsWith(".kt") ->
                        kotlinFiles += name

                    name.endsWith("__after.js") ->
                        jsFilesAfter += name

                    name.endsWith(".js") ->
                        jsFilesBefore += name
                }
            }

            val additionalJsFile = filePath.removeSuffix(".kt") + ".js"
            if (File(additionalJsFile).exists()) {
                jsFilesBefore += additionalJsFile
            }

            val localCommonFile = file.parent + "/" + COMMON_FILES_NAME + "." + KotlinFileType.EXTENSION
            val localCommonFiles = if (File(localCommonFile).exists()) listOf(localCommonFile) else emptyList()

            val allSourceFiles = kotlinFiles + localCommonFiles

            val psiFiles = createPsiFiles(allSourceFiles.map { File(it).canonicalPath }.sorted())
            val config = createConfig(languageVersionSettings)
            val filesToCompile = psiFiles.map { TranslationUnit.SourceFile(it).file }
            val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

            val phaseConfig = if (debugMode >= DebugMode.DEBUG) {
                val allPhasesSet = if (debugMode >= DebugMode.SUPER_DEBUG) wasmPhases.toPhaseMap().values.toSet() else emptySet()
                val dumpOutputDir = File(outputWatFile.parent, outputWatFile.nameWithoutExtension + "-irdump")
                println("\n ------ Dumping phases to file://$dumpOutputDir")
                println(" ------ KT   file://${file.absolutePath}")
                println(" ------ WAT  file://$outputWatFile")
                println(" ------ WASM file://$outputWasmFile")
                println(" ------ JS   file://$outputJsFile")
                PhaseConfig(
                    wasmPhases,
                    dumpToDirectory = dumpOutputDir.path,
                    toDumpStateAfter = allPhasesSet,
                    // toValidateStateAfter = allPhasesSet,
                    // dumpOnlyFqName = null
                )
            } else {
                PhaseConfig(wasmPhases)
            }

            val sourceModule = prepareAnalyzedSourceModule(
                config.project,
                filesToCompile,
                config.configuration,
                // TODO: Bypass the resolver fow wasm.
                listOf(System.getProperty("kotlin.wasm.stdlib.path")!!, System.getProperty("kotlin.wasm.kotlin.test.path")!!),
                emptyList(),
                AnalyzerWithCompilerReport(config.configuration)
            )

            compileAndRun(
                phaseConfig = phaseConfig,
                sourceModule = sourceModule,
                testPackage = testPackage,
                dceEnabled = false,
                outputWatFile = outputWatNoDceFile,
                outputWasmFile = outputWasmNoDceFile,
                outputJsFile = outputJsNoDceFile,
                outputBrowserDir = outputBrowserNoDceDir,
                debugMode = debugMode,
                jsFilesBefore = jsFilesBefore,
                jsFilesAfter = jsFilesAfter
            )

            compileAndRun(
                phaseConfig = phaseConfig,
                sourceModule = sourceModule,
                testPackage = testPackage,
                dceEnabled = true,
                outputBrowserDir = outputBrowserDir,
                debugMode = debugMode,
                outputWatFile = outputWatFile,
                outputWasmFile = outputWasmFile,
                outputJsFile = outputJsFile,
                jsFilesBefore = jsFilesBefore,
                jsFilesAfter = jsFilesAfter
            )
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

    private fun compileAndRun(
        phaseConfig: PhaseConfig,
        sourceModule: ModulesStructure,
        testPackage: String?,
        dceEnabled: Boolean,
        outputWatFile: File,
        outputWasmFile: File,
        outputJsFile: File,
        outputBrowserDir: File,
        debugMode: DebugMode,
        jsFilesBefore: List<String>,
        jsFilesAfter: List<String>,
    ) {
        val (allModules, backendContext) = compileToLoweredIr(
            depsDescriptors = sourceModule,
            phaseConfig = phaseConfig,
            irFactory = IrFactoryImpl,
            exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, TEST_FUNCTION))),
            propertyLazyInitialization = true,
        )

        val compilerResult = compileWasm(
            allModules = allModules,
            backendContext = backendContext,
            emitNameSection = true,
            dceEnabled = dceEnabled,
        )

        outputWatFile.write(compilerResult.wat)
        outputWasmFile.writeBytes(compilerResult.wasm)

        val testRunner = """
            const wasmBinary = read(String.raw`${outputWasmFile.absoluteFile}`, 'binary');
            const wasmModule = new WebAssembly.Module(wasmBinary);
            wasmInstance = new WebAssembly.Instance(wasmModule, { js_code });
            const ${sanitizeName(TEST_MODULE)} = wasmInstance.exports;
            ${createJsRun(wasmInstance = "wasmInstance", dceEnabled = dceEnabled)}
        """.trimIndent()
        outputJsFile.write(compilerResult.js + "\n" + testRunner)

        if (debugMode >= DebugMode.SUPER_DEBUG) {
            createDirectoryToRunInBrowser(outputBrowserDir, compilerResult, dceEnabled)
        }

        ExternalTool(System.getProperty("javascript.engine.path.V8"))
            .run(
                "--experimental-wasm-typed-funcref",
                "--experimental-wasm-gc",
                "--experimental-wasm-eh",
                *jsFilesBefore.toTypedArray(),
                outputJsFile.absolutePath,
                *jsFilesAfter.toTypedArray(),
            )
    }

    private fun createJsRun(wasmInstance: String, dceEnabled: Boolean) = """
            let actualResult
            try {
                $wasmInstance.exports.__init();
                $wasmInstance.exports.startUnitTests?.();
                actualResult = $wasmInstance.exports.$TEST_FUNCTION();
            } catch(e) {
                console.log('Failed with exception!')
                console.log('Message: ' + e.message)
                console.log('Name:    ' + e.name)
                console.log('Stack:')
                console.log(e.stack)
            }
            if (actualResult !== "OK")
                throw `Wrong box result '${'$'}{actualResult}' (with DCE=${dceEnabled}); Expected "OK"`;
    """.trimIndent()

    private fun createDirectoryToRunInBrowser(directory: File, compilerResult: WasmCompilerResult, dceEnabled: Boolean) {
        val browserRunner =
            """
            const response = await fetch("index.wasm");
            const wasmBinary = await response.arrayBuffer();
            wasmInstance = (await WebAssembly.instantiate(wasmBinary, { js_code })).instance;
            ${createJsRun(wasmInstance = "wasmInstance", dceEnabled = dceEnabled)}
            console.log("Test passed!");    
            """.trimIndent()

        directory.mkdirs()

        File(directory, "index.html").writeText(
            """
            <!DOCTYPE html>
            <html lang="en">
            <body>
            <script src="index.js" type="module"></script>
            </body>
            </html>
            """.trimIndent()
        )
        File(directory, "index.js").writeText(
            compilerResult.js + "\n" + browserRunner
        )
        File(directory, "index.wasm").writeBytes(
            compilerResult.wasm
        )
    }

    private fun createConfig(languageVersionSettings: LanguageVersionSettings?): JsConfig {
        val configuration = environment.configuration.copy()
        configuration.put(CommonConfigurationKeys.MODULE_NAME, TEST_MODULE)
        configuration.languageVersionSettings = languageVersionSettings
            ?: LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
        return JsConfig(project, configuration, CompilerEnvironment, null, null)
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

            val languageVersionSettings = parseLanguageVersionSettings(directives)

            val temporaryFile = File(tmpDir, "WASM_TEST/$fileName")
            KtTestUtil.mkdirs(temporaryFile.parentFile)
            temporaryFile.writeText(text, Charsets.UTF_8)

            return TestFile(temporaryFile.absolutePath, languageVersionSettings)
        }

        var testPackage: String? = null
        val tmpDir = KtTestUtil.tmpDir("wasm-tests")

        override fun close() {
            FileUtil.delete(tmpDir)
        }
    }

    private class TestFile(val fileName: String, val languageVersionSettings: LanguageVersionSettings?)

    override fun createEnvironment() =
        KotlinCoreEnvironment.createForTests(testRootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)

    private fun KotlinTestWithEnvironment.createPsiFile(fileName: String): KtFile {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")

        return psiManager.findFile(file) as KtFile
    }

    private fun KotlinTestWithEnvironment.createPsiFiles(fileNames: List<String>): List<KtFile> {
        return fileNames.map { this@createPsiFiles.createPsiFile(it) }
    }

    companion object {
        const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"
        const val TEST_MODULE = "main"
        private const val TEST_FUNCTION = "box"
    }
}

private fun File.write(text: String) {
    parentFile.mkdirs()
    writeText(text)
}
