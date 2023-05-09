/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.toPhaseMap
import org.jetbrains.kotlin.backend.wasm.*
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.checkers.parseLanguageVersionSettings
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.js.klib.TopDownAnalyzerFacadeForWasm
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.ir.backend.js.dce.dumpDeclarationIrSizesIfNeed
import org.jetbrains.kotlin.ir.backend.js.prepareAnalyzedSourceModule
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.Closeable
import java.io.File

abstract class BasicWasmBoxTest(
    private val pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    private val startUnitTests: Boolean = false
) : KotlinTestWithEnvironment() {

    private val pathToRootOutputDir: String = System.getProperty("kotlin.wasm.test.root.out.dir") ?: error("'kotlin.wasm.test.root.out.dir' is not set")

    private val testGroupOutputDirForCompilation = File(pathToRootOutputDir + "out/" + testGroupOutputDirPrefix)

    private val COMMON_FILES_NAME = "_common"
    private val COMMON_FILES_DIR = "_commonFiles"

    private val extraLanguageFeatures = mapOf(
        LanguageFeature.JsAllowImplementingFunctionInterface to LanguageFeature.State.ENABLED,
    )

    fun doTest(filePath: String) = doTestWithTransformer(filePath) { it }

    @OptIn(ObsoleteTestInfrastructure::class)
    fun doTestWithTransformer(filePath: String, transformer: java.util.function.Function<String, String>) {
        val file = File(filePath)

        val outputDirBase = File(getOutputDir(file), getTestName(true))
        val fileContent = transformer.apply(KtTestUtil.doLoadFile(file))

        TestFileFactoryImpl().use { testFactory ->
            val inputFiles: MutableList<TestFile> = TestFiles.createTestFiles(file.name, fileContent, testFactory, true)
            val testPackage = testFactory.testPackage

            val languageVersionSettings = inputFiles.firstNotNullOfOrNull { it.languageVersionSettings }

            val kotlinFiles = mutableListOf<String>()
            val jsFiles = mutableListOf<String>()
            val mjsFiles = mutableListOf<String>()

            var entryMjs: String? = "test.mjs"

            inputFiles.forEach {
                val name = it.fileName
                when {
                    name.endsWith(".kt") ->
                        kotlinFiles += name

                    name.endsWith(".js") ->
                        jsFiles += name

                    name.endsWith(".mjs") -> {
                        mjsFiles += name
                        val fileName = File(name).name
                        if (fileName == "entry.mjs") {
                            entryMjs = fileName
                        }
                    }
                }
            }

            val additionalJsFile = filePath.removeSuffix(".kt") + ".js"
            if (File(additionalJsFile).exists()) {
                jsFiles += additionalJsFile
            }
            val additionalMjsFile = filePath.removeSuffix(".kt") + ".mjs"
            if (File(additionalMjsFile).exists()) {
                mjsFiles += additionalMjsFile
            }

            val localCommonFile = file.parent + "/" + COMMON_FILES_NAME + "." + KotlinFileType.EXTENSION
            val localCommonFiles = if (File(localCommonFile).exists()) listOf(localCommonFile) else emptyList()

            val globalCommonFilesDir = File(File(pathToTestDir).parent, COMMON_FILES_DIR)
            val globalCommonFiles = globalCommonFilesDir.listFiles().orEmpty().map { it.absolutePath }

            val allSourceFiles = kotlinFiles + localCommonFiles + globalCommonFiles

            val psiFiles = createPsiFiles(allSourceFiles.map { File(it).canonicalPath }.sorted())
            val config = createConfig(languageVersionSettings)
            val filesToCompile = psiFiles.map { TranslationUnit.SourceFile(it).file }
            val debugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode")

            val phaseConfig = if (debugMode >= DebugMode.SUPER_DEBUG) {
                val dumpOutputDir = File(outputDirBase, "irdump")
                println("\n ------ Dumping phases to file://${dumpOutputDir.absolutePath}")
                PhaseConfig(
                    wasmPhases,
                    dumpToDirectory = dumpOutputDir.path,
                    toDumpStateAfter = wasmPhases.toPhaseMap().values.toSet(),
                )
            } else {
                PhaseConfig(wasmPhases)
            }

            if (debugMode >= DebugMode.DEBUG) {
                println(" ------ KT   file://${file.absolutePath}")
            }

            val sourceModule = prepareAnalyzedSourceModule(
                config.project,
                filesToCompile,
                config.configuration,
                // TODO: Bypass the resolver fow wasm.
                listOf(System.getProperty("kotlin.wasm.stdlib.path")!!, System.getProperty("kotlin.wasm.kotlin.test.path")!!),
                emptyList(),
                AnalyzerWithCompilerReport(config.configuration),
                analyzerFacade = TopDownAnalyzerFacadeForWasm
            )

            val (allModules, backendContext) = compileToLoweredIr(
                depsDescriptors = sourceModule,
                phaseConfig = phaseConfig,
                irFactory = IrFactoryImpl,
                exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, TEST_FUNCTION))),
                propertyLazyInitialization = true,
            )

            val generateWat = debugMode >= DebugMode.DEBUG
            val baseFileName = "index"

            val compilerResult = compileWasm(
                allModules = allModules,
                backendContext = backendContext,
                baseFileName = baseFileName,
                emitNameSection = true,
                allowIncompleteImplementations = false,
                generateWat = generateWat,
            )

            eliminateDeadDeclarations(allModules, backendContext)

            dumpDeclarationIrSizesIfNeed(System.getProperty("kotlin.wasm.dump.declaration.ir.size.to.file"), allModules)

            val compilerResultWithDCE = compileWasm(
                allModules = allModules,
                backendContext = backendContext,
                baseFileName = baseFileName,
                emitNameSection = true,
                allowIncompleteImplementations = true,
                generateWat = generateWat,
            )

            val testJsQuiet = """
                let actualResult;
                try {
                    // Use "dynamic import" to catch exception happened during JS & Wasm modules initialization
                    let jsModule = await import('./index.mjs');
                    let wasmExports = jsModule.default;
                    ${if (startUnitTests) "wasmExports.startUnitTests();" else ""}
                    actualResult = wasmExports.box();
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

            fun printPathAndSize(mode: String, fileKind: String, path: String, name: String) {
                val size = File("$path/$name").length()
                println(" ------ $mode $fileKind file://$path/$name $size B")
            }

            fun checkExpectedOutputSize(testFileContent: String, testDir: File) {
                val expectedSizes =
                    InTextDirectivesUtils.findListWithPrefixes(testFileContent, "// WASM_DCE_EXPECTED_OUTPUT_SIZE: ")
                        .map {
                            val i = it.indexOf(' ')
                            val extension = it.substring(0, i)
                            val size = it.substring(i + 1)
                            extension.trim().lowercase() to size.filter(Char::isDigit).toInt()
                        }

                val filesByExtension = testDir.listFiles()?.groupBy { it.extension }.orEmpty()

                val errors = expectedSizes.mapNotNull { (extension, expectedSize) ->
                    val totalSize = filesByExtension[extension].orEmpty().sumOf { it.length() }

                    val thresholdPercent = 1
                    val thresholdInBytes = expectedSize * thresholdPercent / 100

                    val expectedMinSize = expectedSize - thresholdInBytes
                    val expectedMaxSize = expectedSize + thresholdInBytes

                    val diff = totalSize - expectedSize

                    val message = "Total size of $extension files is $totalSize," +
                            " but expected $expectedSize ∓ $thresholdInBytes [$expectedMinSize .. $expectedMaxSize]." +
                            " Diff: $diff (${diff * 100 / expectedSize}%)"

                    if (debugMode >= DebugMode.DEBUG) {
                        println(" ------ $message")
                    }

                    if (totalSize !in expectedMinSize..expectedMaxSize) message else null
                }

                if (errors.isNotEmpty()) throw AssertionError(errors.joinToString("\n"))
            }

            fun writeToFilesAndRunTest(mode: String, res: WasmCompilerResult) {
                val dir = File(outputDirBase, mode)
                dir.mkdirs()

                writeCompilationResult(res, dir, baseFileName)
                File(dir, "test.mjs").writeText(testJs)

                for (mjsPath: String in mjsFiles) {
                    val mjsFile = File(mjsPath)
                    File(dir, mjsFile.name).writeText(mjsFile.readText())
                }

                if (debugMode >= DebugMode.DEBUG) {
                    File(dir, "index.html").writeText(
                        """
                            <!DOCTYPE html>
                            <html lang="en">
                            <body>
                                <span id="test">UNKNOWN</span>
                                <script type="module">
                                    let test = document.getElementById("test")
                                    try {
                                        await import("./test.mjs");
                                        test.style.backgroundColor = "#0f0";
                                        test.textContent = "OK"
                                    } catch(e) {
                                        test.style.backgroundColor = "#f00";
                                        test.textContent = "NOT OK"
                                        throw e;
                                    }
                                </script>
                            </body>
                            </html>
                        """.trimIndent()
                    )

                    val path = dir.absolutePath
                    println(" ------ $mode Wat  file://$path/index.wat")
                    println(" ------ $mode Wasm file://$path/index.wasm")
                    println(" ------ $mode JS   file://$path/index.uninstantiated.mjs")
                    println(" ------ $mode JS   file://$path/index.mjs")
                    println(" ------ $mode Test file://$path/test.mjs")
                    val projectName = "kotlin"
                    println(" ------ $mode HTML http://0.0.0.0:63342/$projectName/${dir.path}/index.html")
                    for (mjsPath: String in mjsFiles) {
                        println(" ------ $mode External ESM file://$path/${File(mjsPath).name}")
                    }
                }

                val testFile = file.readText()

                val failsIn = InTextDirectivesUtils.findListWithPrefixes(testFile, "// WASM_FAILS_IN: ")

                val exceptions = listOf(WasmVM.V8, WasmVM.SpiderMonkey).mapNotNull map@{ vm ->
                    try {
                        if (debugMode >= DebugMode.DEBUG) {
                            println(" ------ Run in ${vm.name}" + if (vm.shortName in failsIn) " (expected to fail)" else "")
                        }
                        vm.run(
                            "./${entryMjs}",
                            jsFiles.map { File(it).absolutePath },
                            workingDirectory = dir
                        )
                        if (vm.shortName in failsIn) {
                            return@map AssertionError("The test expected to fail in ${vm.name}. Please update the testdata.")
                        }
                    } catch (e: Throwable) {
                        if (vm.shortName !in failsIn) {
                            return@map e
                        }
                    }
                    null
                }

                when (exceptions.size) {
                    0 -> {} // Everything OK
                    1 -> {
                        throw exceptions.single()
                    }
                    else -> {
                        throw AssertionError("Failed with several exceptions. Look at suppressed exceptions below.").apply {
                            exceptions.forEach { addSuppressed(it) }
                        }
                    }
                }

                if (mode == "dce") {
                    checkExpectedOutputSize(testFile, dir)
                }
            }

            writeToFilesAndRunTest("dev", compilerResult)
            writeToFilesAndRunTest("dce", compilerResultWithDCE)
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
        configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.ES)
        configuration.languageVersionSettings = languageVersionSettings
            ?: LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE, specificFeatures = extraLanguageFeatures)
        return JsConfig(project, configuration, CompilerEnvironment, null, null)
    }

    @OptIn(ObsoleteTestInfrastructure::class)
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

            val languageVersionSettings = parseLanguageVersionSettings(directives, extraLanguageFeatures)

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
        const val TEST_MODULE = "main"
        private const val TEST_FUNCTION = "box"
    }
}
