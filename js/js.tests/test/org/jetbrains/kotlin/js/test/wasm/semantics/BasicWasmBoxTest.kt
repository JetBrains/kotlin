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
import org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettings
import org.jetbrains.kotlin.checkers.parseLanguageVersionSettings
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.ir.backend.js.generateKLib
import org.jetbrains.kotlin.ir.backend.js.jsPhases
import org.jetbrains.kotlin.ir.backend.js.loadKlib
import org.jetbrains.kotlin.ir.backend.js.wasm.compileWasm
import org.jetbrains.kotlin.ir.backend.js.wasm.wasmPhases
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.js.test.NashornJsTestChecker
import org.jetbrains.kotlin.js.test.V8JsTestChecker
import org.jetbrains.kotlin.js.test.utils.JsTestUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestUtils.TestFileFactory
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.DFS
import java.io.Closeable
import java.io.File
import java.lang.Boolean.getBoolean
import java.util.regex.Pattern

private val wasmRuntimeKlib =
    loadKlib("compiler/ir/serialization.js/build/wasmRuntime/klib")


open class BasicWasmBoxTest(
    private val pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    pathToRootOutputDir: String = TEST_DATA_DIR_PATH
) : KotlinTestWithEnvironment() {
    private val additionalCommonFileDirectories = mutableListOf<String>()

    private val testGroupOutputDirForCompilation = File(pathToRootOutputDir + "out/" + testGroupOutputDirPrefix)

    private val testChecker get() = if (runTestInNashorn) NashornJsTestChecker else V8JsTestChecker

    fun doTest(filePath: String) {
        doTest(filePath, "OK", MainCallParameters.noCall())
    }

    fun doTestWithCoroutinesPackageReplacement(filePath: String, coroutinesPackage: String) {
        doTest(filePath, "OK", MainCallParameters.noCall(), coroutinesPackage)
    }

    fun doTest(filePath: String, expectedResult: String, mainCallParameters: MainCallParameters, coroutinesPackage: String = "") {
        val file = File(filePath)
        val outputDir = getOutputDir(file)
        var fileContent = KotlinTestUtils.doLoadFile(file)
        if (coroutinesPackage.isNotEmpty()) {
            fileContent = fileContent.replace("COROUTINES_PACKAGE", coroutinesPackage)
        }

        val inferMainModule = INFER_MAIN_MODULE.matcher(fileContent).find()
        val actualMainCallParameters = if (CALL_MAIN_PATTERN.matcher(fileContent).find()) MainCallParameters.mainWithArguments(listOf("testArg")) else mainCallParameters

        TestFileFactoryImpl(coroutinesPackage).use { testFactory ->
            val inputFiles = KotlinTestUtils.createTestFiles(file.name, fileContent, testFactory, true, coroutinesPackage)
            val modules = inputFiles
                    .map { it.module }.distinct()
                    .map { it.name to it }.toMap()

            fun TestModule.allTransitiveDependencies(): Set<String> {
                return dependencies.toSet() + dependencies.flatMap { modules.getValue(it).allTransitiveDependencies() }
            }

            val orderedModules = DFS.topologicalOrder(modules.values) { module -> module.dependencies.mapNotNull { modules[it] } }

            val testPackage = testFactory.testPackage

            val testFunction = TEST_FUNCTION

            val mainModuleName = when {
                inferMainModule -> orderedModules.last().name
                TEST_MODULE in modules -> TEST_MODULE
                else -> DEFAULT_MODULE
            }

            val generatedJsFiles = orderedModules.asReversed().mapNotNull { module ->
                val dependencies = module.dependencies.map { modules[it]?.outputFileName(outputDir) + ".meta.js" }
                val allDependencies = module.allTransitiveDependencies().map { modules[it]?.outputFileName(outputDir) + ".meta.js" }
                val friends = module.friends.map { modules[it]?.outputFileName(outputDir) + ".meta.js" }

                val outputFileName = module.outputFileName(outputDir) + ".js"
                val isMainModule = mainModuleName == module.name
                generateJavaScriptFile(
                    file.parent, module, outputFileName, dependencies, allDependencies, friends, modules.size > 1,
                    actualMainCallParameters, testPackage, testFunction,
                    isMainModule
                )

                Pair(outputFileName, module)
            }

            val globalCommonFiles = JsTestUtils.getFilesInDirectoryByExtension(
                COMMON_FILES_DIR_PATH,
                JavaScript.EXTENSION
            )
            val localCommonFile = file.parent + "/" + COMMON_FILES_NAME + JavaScript.DOT_EXTENSION
            val localCommonFiles = if (File(localCommonFile).exists()) listOf(localCommonFile) else emptyList()

            val additionalCommonFiles = additionalCommonFileDirectories.flatMap { baseDir ->
                JsTestUtils.getFilesInDirectoryByExtension("$baseDir/", JavaScript.EXTENSION)
            }
            val inputJsFiles = inputFiles
                    .filter { it.fileName.endsWith(".js") }
                    .map { inputJsFile ->
                        val sourceFile = File(inputJsFile.fileName)
                        val targetFile = File(outputDir, inputJsFile.module.outputFileSimpleName() + "-js-" + sourceFile.name)
                        FileUtil.copy(File(inputJsFile.fileName), targetFile)
                        targetFile.absolutePath
                    }

            val allJsFiles = inputJsFiles + generatedJsFiles.map { it.first } + globalCommonFiles + localCommonFiles +
                             additionalCommonFiles

            val dontRunGeneratedCode = InTextDirectivesUtils.dontRunGeneratedCode(TargetBackend.JS, file)

            if (!dontRunGeneratedCode) {
                testChecker.check(allJsFiles, mainModuleName, testPackage, testFunction, expectedResult, false)
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

    private fun TestModule.outputFileSimpleName(): String {
        val outputFileSuffix = if (this.name == TEST_MODULE) "" else "-$name"
        return getTestName(true) + outputFileSuffix
    }

    private fun TestModule.outputFileName(directory: File) = directory.absolutePath + "/" + outputFileSimpleName() + "_v5"

    private fun generateJavaScriptFile(
        directory: String,
        module: TestModule,
        outputFileName: String,
        dependencies: List<String>,
        allDependencies: List<String>,
        friends: List<String>,
        multiModule: Boolean,
        mainCallParameters: MainCallParameters,
        testPackage: String?,
        testFunction: String,
        isMainModule: Boolean
    ) {
        val kotlinFiles =  module.files.filter { it.fileName.endsWith(".kt") }
        val testFiles = kotlinFiles.map { it.fileName }
        val globalCommonFiles = JsTestUtils.getFilesInDirectoryByExtension(
            COMMON_FILES_DIR_PATH,
            KotlinFileType.EXTENSION
        )
        val localCommonFile = directory + "/" + COMMON_FILES_NAME + "." + KotlinFileType.EXTENSION
        val localCommonFiles = if (File(localCommonFile).exists()) listOf(localCommonFile) else emptyList()
        // TODO probably it's no longer needed.
        val additionalCommonFiles = additionalCommonFileDirectories.flatMap { baseDir ->
            JsTestUtils.getFilesInDirectoryByExtension("$baseDir/", KotlinFileType.EXTENSION)
        }
        val additionalFiles = globalCommonFiles + localCommonFiles + additionalCommonFiles
        val allSourceFiles = (testFiles + additionalFiles).map(::File)
        val psiFiles = createPsiFiles(allSourceFiles.sortedBy { it.canonicalPath }.map { it.canonicalPath })

        val config = createConfig(module, dependencies, allDependencies, friends, multiModule)
        val outputFile = File(outputFileName)

        translateFiles(
            psiFiles.map(TranslationUnit::SourceFile), outputFile, config,
            mainCallParameters, testPackage, testFunction, isMainModule
        )
    }

    fun translateFiles(
        units: List<TranslationUnit>,
        outputFile: File,
        config: JsConfig,
        mainCallParameters: MainCallParameters,
        testPackage: String?,
        testFunction: String,
        isMainModule: Boolean
    ) {
        val filesToCompile = units
            .map { (it as TranslationUnit.SourceFile).file }
            // TODO: split input files to some parts (global common, local common, test)
            .filterNot { it.virtualFilePath.contains(COMMON_FILES_DIR_PATH) }

        val runtimeKlibs = listOf(wasmRuntimeKlib)

        val actualOutputFile = outputFile.absolutePath.let {
            if (!isMainModule) it.replace("_v5.js", "/") else it
        }

        if (isMainModule) {
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
                mainArguments = mainCallParameters.run { if (shouldBeGenerated()) arguments() else null },
                exportedDeclarations = setOf(FqName.fromSegments(listOfNotNull(testPackage, testFunction)))
            )

            outputFile.write(jsCode)
        } else {
            generateKLib(
                project = config.project,
                files = filesToCompile,
                configuration = config.configuration,
                allDependencies = runtimeKlibs,
                friendDependencies = emptyList(),
                outputKlibPath = actualOutputFile,
                nopack = true
            )
        }
    }

    private fun createPsiFile(fileName: String): KtFile {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")

        return psiManager.findFile(file) as KtFile
    }

    private fun createPsiFiles(fileNames: List<String>): List<KtFile> = fileNames.map(this::createPsiFile)

    private fun createConfig(
        module: TestModule,
        dependencies: List<String>,
        allDependencies: List<String>,
        friends: List<String>,
        multiModule: Boolean
    ): JsConfig {
        val configuration = environment.configuration.copy()

        configuration.put(CommonConfigurationKeys.DISABLE_INLINE, module.inliningDisabled)
        module.languageVersionSettings?.let { languageVersionSettings ->
            configuration.languageVersionSettings = languageVersionSettings
        }

        configuration.put(JSConfigurationKeys.LIBRARIES, dependencies)
        configuration.put(JSConfigurationKeys.TRANSITIVE_LIBRARIES, allDependencies)
        configuration.put(JSConfigurationKeys.FRIEND_PATHS, friends)

        configuration.put(CommonConfigurationKeys.MODULE_NAME, module.name.removeSuffix(OLD_MODULE_SUFFIX))
        configuration.put(JSConfigurationKeys.MODULE_KIND, module.moduleKind)
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.v5)
        configuration.put(JSConfigurationKeys.META_INFO, multiModule)

        return JsConfig(project, configuration, null, null)
    }

    private inner class TestFileFactoryImpl(val coroutinesPackage: String) : TestFileFactory<TestModule, TestFile>, Closeable {
        var testPackage: String? = null
        val tmpDir = KotlinTestUtils.tmpDir("js-tests")
        val defaultModule = TestModule(
            TEST_MODULE,
            emptyList(),
            emptyList()
        )
        var languageVersionSettings: LanguageVersionSettings? = null

        override fun createFile(module: TestModule?, fileName: String, text: String, directives: Map<String, String>): TestFile? {
            val currentModule = module ?: defaultModule

            val ktFile = KtPsiFactory(project).createFile(text)
            val boxFunction = ktFile.declarations.find { it is KtNamedFunction && it.name == TEST_FUNCTION }
            if (boxFunction != null) {
                testPackage = ktFile.packageFqName.asString()
                if (testPackage?.isEmpty() == true) {
                    testPackage = null
                }
            }

            val temporaryFile = File(tmpDir, "${currentModule.name}/$fileName")
            KotlinTestUtils.mkdirs(temporaryFile.parentFile)
            temporaryFile.writeText(text, Charsets.UTF_8)

            // TODO Deduplicate logic copied from CodegenTestCase.updateConfigurationByDirectivesInTestFiles
            fun LanguageVersion.toSettings() = CompilerTestLanguageVersionSettings(
                emptyMap(),
                ApiVersion.createByLanguageVersion(this),
                this,
                emptyMap()
            )

            fun LanguageVersionSettings.trySet() {
                val old = languageVersionSettings
                assert(old == null || old == this) { "Should not specify language version settings twice:\n$old\n$this" }
                languageVersionSettings = this
            }
            InTextDirectivesUtils.findStringWithPrefixes(text, "// LANGUAGE_VERSION:")?.let {
                LanguageVersion.fromVersionString(it)?.toSettings()?.trySet()
            }
            if (InTextDirectivesUtils.findLinesWithPrefixesRemoved(text, "// COMMON_COROUTINES_TEST").isNotEmpty()) {
                assert(!text.contains("COROUTINES_PACKAGE")) { "Must replace COROUTINES_PACKAGE prior to tests compilation" }
                if (coroutinesPackage.isNotEmpty()) {
                    if (coroutinesPackage == "kotlin.coroutines.experimental") {
                        LanguageVersion.KOTLIN_1_2.toSettings().trySet()
                    } else {
                        LanguageVersion.KOTLIN_1_3.toSettings().trySet()
                    }
                }
            }

            parseLanguageVersionSettings(directives)?.trySet()

            // Relies on the order of module creation
            // TODO is that ok?
            currentModule.languageVersionSettings = languageVersionSettings

            return TestFile(
                temporaryFile.absolutePath,
                currentModule
            )
        }

        override fun createModule(name: String, dependencies: List<String>, friends: List<String>) =
            TestModule(name, dependencies, friends)

        override fun close() {
            FileUtil.delete(tmpDir)
        }
    }

    private class TestFile(val fileName: String, val module: TestModule) {
        init {
            module.files += this
        }
    }

    private class TestModule(
            val name: String,
            dependencies: List<String>,
            friends: List<String>
    ) {
        val dependencies = dependencies.toMutableList()
        val friends = friends.toMutableList()
        var moduleKind = ModuleKind.PLAIN
        var inliningDisabled = false
        val files = mutableListOf<TestFile>()
        var languageVersionSettings: LanguageVersionSettings? = null
    }

    override fun createEnvironment() =
            KotlinCoreEnvironment.createForTests(testRootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)

    companion object {
        const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"

        private const val COMMON_FILES_NAME = "_common"
        private const val COMMON_FILES_DIR = "_commonFiles/"
        const val COMMON_FILES_DIR_PATH = TEST_DATA_DIR_PATH + COMMON_FILES_DIR

        @JvmStatic
        private val runTestInNashorn = getBoolean("kotlin.js.useNashorn")

        const val TEST_MODULE = "JS_TESTS"
        private const val DEFAULT_MODULE = "main"
        private const val TEST_FUNCTION = "box"
        private const val OLD_MODULE_SUFFIX = "-old"

        private val CALL_MAIN_PATTERN = Pattern.compile("^// *CALL_MAIN *$", Pattern.MULTILINE)
        private val INFER_MAIN_MODULE = Pattern.compile("^// *INFER_MAIN_MODULE", Pattern.MULTILINE)
    }
}

private fun File.write(text: String) {
    parentFile.mkdirs()
    writeText(text)
}
