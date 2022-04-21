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
import org.jetbrains.kotlin.backend.wasm.*
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.checkers.parseLanguageVersionSettings
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.ir.backend.js.prepareAnalyzedSourceModule
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
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

    fun doTest(filePath: String) = doTestWithTransformer(filePath) { it }
    fun doTestWithTransformer(filePath: String, transformer: java.util.function.Function<String, String>) {
        val file = File(filePath)

        val outputDirBase = File(getOutputDir(file), getTestName(true))
        val fileContent = transformer.apply(KtTestUtil.doLoadFile(file))

        TestFileFactoryImpl().use { testFactory ->
            val inputFiles: MutableList<TestFile> = TestFiles.createTestFiles(file.name, fileContent, testFactory, true)
            val testPackage = testFactory.testPackage

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
                val dumpOutputDir = File(outputDirBase, "irdump")
                println("\n ------ Dumping phases to file://${dumpOutputDir.absolutePath}")
                println(" ------ KT   file://${file.absolutePath}")
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
                allowIncompleteImplementations = false,
            )

            eliminateDeadDeclarations(allModules, backendContext)

            val compilerResultWithDCE = compileWasm(
                allModules = allModules,
                backendContext = backendContext,
                emitNameSection = true,
                allowIncompleteImplementations = true,
            )

            val testJsQuiet = """
                import exports from './index.js';
        
                let actualResult
                try {
                    actualResult = exports.box();
                } catch(e) {
                    console.log('Failed with exception!')
                    console.log('Message: ' + e.message)
                    console.log('Name:    ' + e.name)
                    console.log('Stack:')
                    console.log(e.stack)
                }
                if (actualResult !== "OK")
                    throw `Wrong box result '${'$'}{actualResult}'; Expected "OK"`;
            """.trimIndent()

            val testJsVerbose = testJsQuiet + """
                console.log('test passed');
            """.trimIndent()

            val testJs = if (debugMode >= DebugMode.DEBUG) testJsVerbose else testJsQuiet

            fun compileAndRunD8Test(name: String, res: WasmCompilerResult) {
                val dir = File(outputDirBase, name)
                if (debugMode >= DebugMode.DEBUG) {
                    val path = dir.absolutePath
                    println(" ------ $name WAT  file://$path/index.wat")
                    println(" ------ $name WASM file://$path/index.wasm")
                    println(" ------ $name JS   file://$path/index.js")
                    println(" ------ $name Test file://$path/test.js")
                }

                writeCompilationResult(res, dir, WasmLoaderKind.D8)
                File(dir, "test.js").writeText(testJs)
                ExternalTool(System.getProperty("javascript.engine.path.V8"))
                    .run(
                        "--experimental-wasm-typed-funcref",
                        "--experimental-wasm-gc",
                        "--experimental-wasm-eh",
                        *jsFilesBefore.map { File(it).absolutePath }.toTypedArray(),
                        "--module",
                        "./test.js",
                        *jsFilesAfter.map { File(it).absolutePath }.toTypedArray(),
                        workingDirectory = dir
                    )
            }

            compileAndRunD8Test("d8", compilerResult)
            compileAndRunD8Test("d8-dce", compilerResultWithDCE)

            if (debugMode >= DebugMode.SUPER_DEBUG) {
                fun writeBrowserTest(name: String, res: WasmCompilerResult) {
                    val dir = File(outputDirBase, name)
                    writeCompilationResult(res, dir, WasmLoaderKind.BROWSER)
                    File(dir, "test.js").writeText(testJsVerbose)
                    File(dir, "index.html").writeText(
                        """
                            <!DOCTYPE html>
                            <html lang="en">
                            <body>
                            <script src="test.js" type="module"></script>
                            </body>
                            </html>
                        """.trimIndent()
                    )
                    val path = dir.absolutePath
                    println(" ------ $name WAT  file://$path/index.wat")
                    println(" ------ $name WASM file://$path/index.wasm")
                    println(" ------ $name JS   file://$path/index.js")
                    println(" ------ $name TEST file://$path/test.js")
                    println(" ------ $name HTML file://$path/index.html")
                }

                writeBrowserTest("browser", compilerResult)
                writeBrowserTest("browser-dce", compilerResultWithDCE)
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

    private fun createConfig(languageVersionSettings: LanguageVersionSettings?): JsConfig {
        val configuration = environment.configuration.copy()
        configuration.put(CommonConfigurationKeys.MODULE_NAME, TEST_MODULE)
        configuration.put(JSConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, true)
        configuration.put(JSConfigurationKeys.WASM_ENABLE_ASSERTS, true)
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
