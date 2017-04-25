/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.output.outputUtils.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.dce.DeadCodeElimination
import org.jetbrains.kotlin.js.dce.InputFile
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.js.test.utils.DirectiveTestUtils
import org.jetbrains.kotlin.js.test.utils.JsTestUtils
import org.jetbrains.kotlin.js.test.utils.verifyAst
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.serialization.js.PackagesWithHeaderMetadata
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestUtils.TestFileFactory
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.DFS
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.PrintStream
import java.nio.charset.Charset
import java.util.regex.Pattern

abstract class BasicBoxTest(
        protected val pathToTestDir: String,
        private val pathToOutputDir: String,
        private val typedArraysEnabled: Boolean = false,
        private val generateSourceMap: Boolean = false,
        private val generateNodeJsRunner: Boolean = true
) : KotlinTestWithEnvironment() {
    val additionalCommonFileDirectories = mutableListOf<String>()

    protected open fun getOutputPrefixFile(testFilePath: String): File? = null
    protected open fun getOutputPostfixFile(testFilePath: String): File? = null

    protected open val runMinifierByDefault: Boolean = false

    fun doTest(filePath: String) {
        doTest(filePath, "OK", MainCallParameters.noCall())
    }

    fun doTest(filePath: String, expectedResult: String, mainCallParameters: MainCallParameters) {
        val file = File(filePath)
        val outputDir = getOutputDir(file)
        val fileContent = KotlinTestUtils.doLoadFile(file)

        val outputPrefixFile = getOutputPrefixFile(filePath)
        val outputPostfixFile = getOutputPostfixFile(filePath)

        TestFileFactoryImpl().use { testFactory ->
            testFactory.defaultModule.moduleKind

            val inputFiles = KotlinTestUtils.createTestFiles(file.name, fileContent, testFactory)
            val modules = inputFiles
                    .map { it.module }.distinct()
                    .map { it.name to it }.toMap()

            val orderedModules = DFS.topologicalOrder(modules.values) { module -> module.dependencies.mapNotNull { modules[it] } }

            val generatedJsFiles = orderedModules.asReversed().mapNotNull { module ->
                val dependencies = module.dependencies.mapNotNull { modules[it]?.outputFileName(outputDir) + ".meta.js" }
                val friends = module.friends.mapNotNull { modules[it]?.outputFileName(outputDir) + ".meta.js" }

                val outputFileName = module.outputFileName(outputDir) + ".js"
                generateJavaScriptFile(file.parent, module, outputFileName, dependencies, friends, modules.size > 1,
                                       outputPrefixFile, outputPostfixFile, mainCallParameters)

                if (!module.name.endsWith(OLD_MODULE_SUFFIX)) Pair(outputFileName, module) else null
            }

            val mainModuleName = if (TEST_MODULE in modules) TEST_MODULE else DEFAULT_MODULE
            val mainModule = modules[mainModuleName]!!

            val globalCommonFiles = JsTestUtils.getFilesInDirectoryByExtension(
                    TEST_DATA_DIR_PATH + COMMON_FILES_DIR, JavaScript.EXTENSION)
            val localCommonFile = file.parent + "/" + COMMON_FILES_NAME + JavaScript.DOT_EXTENSION
            val localCommonFiles = if (File(localCommonFile).exists()) listOf(localCommonFile) else emptyList()

            val additionalCommonFiles = additionalCommonFileDirectories.flatMap { baseDir ->
                JsTestUtils.getFilesInDirectoryByExtension(baseDir + "/", JavaScript.EXTENSION)
            }
            val inputJsFiles = inputFiles
                    .filter { it.fileName.endsWith(".js") }
                    .map { file ->
                        val sourceFile = File(file.fileName)
                        val targetFile = File(outputDir, file.module.outputFileSimpleName() + "-js-" + sourceFile.name)
                        FileUtil.copy(File(file.fileName), targetFile)
                        targetFile.absolutePath
                    }

            val additionalFiles = mutableListOf<String>()

            val moduleKindMatcher = MODULE_KIND_PATTERN.matcher(fileContent)
            val moduleKind = if (moduleKindMatcher.find()) ModuleKind.valueOf(moduleKindMatcher.group(1)) else ModuleKind.PLAIN

            val withModuleSystem = moduleKind != ModuleKind.PLAIN && !NO_MODULE_SYSTEM_PATTERN.matcher(fileContent).find()

            if (withModuleSystem) {
                additionalFiles += MODULE_EMULATION_FILE
            }

            val additionalJsFile = filePath.removeSuffix("." + KotlinFileType.EXTENSION) + JavaScript.DOT_EXTENSION
            if (File(additionalJsFile).exists()) {
                additionalFiles += additionalJsFile
            }

            val allJsFiles = additionalFiles + inputJsFiles + generatedJsFiles.map { it.first } + globalCommonFiles + localCommonFiles +
                             additionalCommonFiles

            if (generateNodeJsRunner && !SKIP_NODE_JS.matcher(fileContent).find()) {
                val nodeRunnerName = mainModule.outputFileName(outputDir) + ".node.js"
                val ignored = InTextDirectivesUtils.isIgnoredTarget(TargetBackend.JS, file)
                val nodeRunnerText = generateNodeRunner(allJsFiles, outputDir, mainModuleName, ignored, testFactory.testPackage)
                FileUtil.writeToFile(File(nodeRunnerName), nodeRunnerText)
            }

            runGeneratedCode(allJsFiles, mainModuleName, testFactory.testPackage, TEST_FUNCTION, expectedResult, withModuleSystem)

            performAdditionalChecks(generatedJsFiles.map { it.first }, outputPrefixFile, outputPostfixFile)

            val expectedReachableNodesMatcher = EXPECTED_REACHABLE_NODES.matcher(fileContent)
            val expectedReachableNodesFound = expectedReachableNodesMatcher.find()
            val skipMinification = System.getProperty("kotlin.js.skipMinificationTest", "false").toBoolean()
            if (!skipMinification &&
                (runMinifierByDefault || expectedReachableNodesFound) &&
                !SKIP_MINIFICATION.matcher(fileContent).find()
            ) {
                val thresholdChecker: (Int) -> Unit = { reachableNodesCount ->
                    val replacement = "// $EXPECTED_REACHABLE_NODES_DIRECTIVE: $reachableNodesCount"
                    if (!expectedReachableNodesFound) {
                        file.writeText("$replacement\n$fileContent")
                        fail("The number of expected reachable nodes was not set. Actual reachable nodes: $reachableNodesCount")
                    }
                    else {
                        val expectedReachableNodes = expectedReachableNodesMatcher.group(1).toInt()
                        val minThreshold = expectedReachableNodes * 9 / 10
                        val maxThreshold = expectedReachableNodes * 11 / 10
                        if (reachableNodesCount < minThreshold || reachableNodesCount > maxThreshold) {

                            val newText = fileContent.substring(0, expectedReachableNodesMatcher.start()) +
                                          replacement +
                                          fileContent.substring(expectedReachableNodesMatcher.end())
                            file.writeText(newText)
                            fail("Number of reachable nodes ($reachableNodesCount) does not fit into expected range " +
                                 "[$minThreshold; $maxThreshold]")
                        }
                    }
                }

                minifyAndRun(
                        workDir = File(File(outputDir, "min"), file.nameWithoutExtension),
                        allJsFiles = allJsFiles,
                        generatedJsFiles = generatedJsFiles,
                        expectedResult = expectedResult,
                        testModuleName = mainModuleName,
                        testPackage = testFactory.testPackage,
                        testFunction = TEST_FUNCTION,
                        withModuleSystem = withModuleSystem,
                        minificationThresholdChecker =  thresholdChecker)
            }
        }
    }

    protected open fun runGeneratedCode(
            jsFiles: List<String>,
            testModuleName: String,
            testPackage: String?,
            testFunction: String,
            expectedResult: String,
            withModuleSystem: Boolean
    ) {
        NashornJsTestChecker.check(jsFiles, testModuleName, testPackage, testFunction, expectedResult, withModuleSystem)
    }

    protected open fun performAdditionalChecks(generatedJsFiles: List<String>, outputPrefixFile: File?, outputPostfixFile: File?) {}

    private fun generateNodeRunner(
            files: Collection<String>,
            dir: File,
            moduleName: String,
            ignored: Boolean,
            testPackage: String?
    ): String {
        val filesToLoad = files.map { FileUtil.getRelativePath(dir, File(it))!!.replace(File.separatorChar, '/') }.map { "\"$it\"" }
        val fqn = testPackage?.let { ".$it" } ?: ""
        val loadAndRun = "load([${filesToLoad.joinToString(",")}], '$moduleName')$fqn.box()"

        val sb = StringBuilder()
        sb.append("module.exports = function(load) {\n")
        if (ignored) {
            sb.append("  try {\n")
            sb.append("    var result = $loadAndRun;\n")
            sb.append("    if (result != 'OK') return 'OK';")
            sb.append("    return 'fail: expected test failure';\n")
            sb.append("  }\n")
            sb.append("  catch (e) {\n")
            sb.append("    return 'OK';\n")
            sb.append("}\n")
        }
        else {
            sb.append("  return $loadAndRun;\n")
        }
        sb.append("};\n")

        return sb.toString()
    }

    protected fun getOutputDir(file: File): File {
        val stopFile = File(pathToTestDir)
        return generateSequence(file.parentFile) { it.parentFile }
                .takeWhile { it != stopFile }
                .map { it.name }
                .toList().asReversed()
                .fold(File(pathToOutputDir), ::File)
    }

    private fun TestModule.outputFileSimpleName(): String {
        val outputFileSuffix = if (this.name == TEST_MODULE) "" else "-$name"
        return getTestName(true) + outputFileSuffix
    }

    private fun TestModule.outputFileName(directory: File): String {
        return directory.absolutePath + "/" + outputFileSimpleName() + "_v5"
    }

    private fun generateJavaScriptFile(
            directory: String,
            module: TestModule,
            outputFileName: String,
            dependencies: List<String>,
            friends: List<String>,
            multiModule: Boolean,
            outputPrefixFile: File?,
            outputPostfixFile: File?,
            mainCallParameters: MainCallParameters
    ) {
        val kotlinFiles =  module.files.filter { it.fileName.endsWith(".kt") }
        val testFiles = kotlinFiles.map { it.fileName }
        val globalCommonFiles = JsTestUtils.getFilesInDirectoryByExtension(
                TEST_DATA_DIR_PATH + COMMON_FILES_DIR, KotlinFileType.EXTENSION)
        val localCommonFile = directory + "/" + COMMON_FILES_NAME + "." + KotlinFileType.EXTENSION
        val localCommonFiles = if (File(localCommonFile).exists()) listOf(localCommonFile) else emptyList()
        val additionalCommonFiles = additionalCommonFileDirectories.flatMap { baseDir ->
            JsTestUtils.getFilesInDirectoryByExtension(baseDir + "/", KotlinFileType.EXTENSION)
        }
        val additionalFiles = globalCommonFiles + localCommonFiles + additionalCommonFiles
        val psiFiles = createPsiFiles(testFiles + additionalFiles)

        val config = createConfig(module, dependencies, friends, multiModule, additionalMetadata = null)
        val outputFile = File(outputFileName)

        translateFiles(psiFiles.map(TranslationUnit::SourceFile), outputFile, config, outputPrefixFile, outputPostfixFile, mainCallParameters)

        if (module.hasFilesToRecompile) {
            val incrementalDir = File(outputFile.parentFile, "incremental/" + outputFile.nameWithoutExtension)
            val serializedMetadata = mutableListOf<File>()
            val translationUnits = kotlinFiles.withIndex().map { (index, file) ->
                if (file.recompile) {
                    TranslationUnit.SourceFile(createPsiFile(file.fileName))
                }
                else {
                    serializedMetadata += File(incrementalDir, "$index.$METADATA_EXTENSION")
                    val astFile = File(incrementalDir, "$index.$AST_EXTENSION")
                    TranslationUnit.BinaryAst(FileUtil.loadFileBytes(astFile))
                }
            }
            val allTranslationUnits = translationUnits + additionalFiles.withIndex().map { (index, _) ->
                val astFile = File(incrementalDir, "${index + translationUnits.size}.$AST_EXTENSION")
                TranslationUnit.BinaryAst(FileUtil.loadFileBytes(astFile))
            }

            val headerFile = File(incrementalDir, HEADER_FILE)
            val recompiledConfig = createConfig(module, dependencies, friends, multiModule, Pair(headerFile,serializedMetadata))
            val recompiledOutputFile = File(outputFile.parentFile, outputFile.nameWithoutExtension + "-recompiled.js")

            translateFiles(allTranslationUnits, recompiledOutputFile, recompiledConfig, outputPrefixFile, outputPostfixFile,
                           mainCallParameters)

            val originalOutput = FileUtil.loadFile(outputFile)
            val recompiledOutput = removeRecompiledSuffix(FileUtil.loadFile(recompiledOutputFile))
            TestCase.assertEquals("Output file changed after recompilation", originalOutput, recompiledOutput)

            val originalSourceMap = FileUtil.loadFile(File(outputFile.parentFile, outputFile.name + ".map"))
            val recompiledSourceMap = removeRecompiledSuffix(
                    FileUtil.loadFile(File(recompiledOutputFile.parentFile, recompiledOutputFile.name + ".map")))
            TestCase.assertEquals("Source map file changed after recompilation", originalSourceMap, recompiledSourceMap)
        }
    }

    private fun removeRecompiledSuffix(text: String): String = text.replace("-recompiled.js", ".js")

    protected fun translateFiles(units: List<TranslationUnit>, outputFile: File, config: JsConfig,
            outputPrefixFile: File?,
            outputPostfixFile: File?,
            mainCallParameters: MainCallParameters) {
        val translator = K2JSTranslator(config)
        val translationResult = translator.translateUnits(units, mainCallParameters)

        if (translationResult !is TranslationResult.Success) {
            val outputStream = ByteArrayOutputStream()
            val collector = PrintingMessageCollector(PrintStream(outputStream), MessageRenderer.PLAIN_FULL_PATHS, true)
            AnalyzerWithCompilerReport.reportDiagnostics(translationResult.diagnostics, collector)
            val messages = outputStream.toByteArray().toString(Charset.forName("UTF-8"))
            throw AssertionError("The following errors occurred compiling test:\n" + messages)
        }

        val outputFiles = translationResult.getOutputFiles(outputFile, outputPrefixFile, outputPostfixFile)
        val outputDir = outputFile.parentFile ?: error("Parent file for output file should not be null, outputFilePath: " + outputFile.path)
        outputFiles.writeAllTo(outputDir)

        if (config.moduleKind == ModuleKind.COMMON_JS) {
            val content = FileUtil.loadFile(outputFile, true)
            val wrappedContent = "$KOTLIN_TEST_INTERNAL.beginModule();\n" +
                                 "$content\n" +
                                 "$KOTLIN_TEST_INTERNAL.endModule(\"${StringUtil.escapeStringCharacters(config.moduleId)}\");"
            FileUtil.writeToFile(outputFile, wrappedContent)
        }
        else if (config.moduleKind == ModuleKind.AMD || config.moduleKind == ModuleKind.UMD) {
            val content = FileUtil.loadFile(outputFile, true)
            val wrappedContent = "if (typeof $KOTLIN_TEST_INTERNAL !== \"undefined\") { " +
                                 "$KOTLIN_TEST_INTERNAL.setModuleId(\"${StringUtil.escapeStringCharacters(config.moduleId)}\"); }\n" +
                                 "$content\n"
            FileUtil.writeToFile(outputFile, wrappedContent)
        }

        val incrementalDir = File(outputDir, "incremental/${outputFile.nameWithoutExtension}")

        for ((index, unit) in units.withIndex()) {
            if (unit !is TranslationUnit.SourceFile) continue
            val fileTranslationResult = translationResult.fileTranslationResults[unit.file]!!
            val basePath = "$index."
            val binaryAst = fileTranslationResult.binaryAst
            val binaryMetadata = fileTranslationResult.metadata

            if (binaryAst != null) {
                FileUtil.writeToFile(File(incrementalDir, basePath + AST_EXTENSION), binaryAst)
            }
            if (binaryMetadata != null) {
                FileUtil.writeToFile(File(incrementalDir, basePath + METADATA_EXTENSION), binaryMetadata)
            }
        }

        translationResult.metadataHeader?.let {
            FileUtil.writeToFile(File(incrementalDir, HEADER_FILE), it)
        }

        processJsProgram(translationResult.program, units.filterIsInstance<TranslationUnit.SourceFile>().map { it.file })
    }

    protected fun processJsProgram(program: JsProgram, psiFiles: List<KtFile>) {
        psiFiles.asSequence()
                .map { it.text }
                .forEach { DirectiveTestUtils.processDirectives(program, it) }
        program.verifyAst()
    }

    private fun createPsiFile(fileName: String): KtFile {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        return psiManager.findFile(fileSystem.findFileByPath(fileName)!!) as KtFile
    }

    private fun createPsiFiles(fileNames: List<String>): List<KtFile> = fileNames.map(this::createPsiFile)

    private fun createConfig(
            module: TestModule, dependencies: List<String>, friends: List<String>, multiModule: Boolean, additionalMetadata: Pair<File, List<File>>?
    ): JsConfig {
        val configuration = environment.configuration.copy()

        configuration.put(CommonConfigurationKeys.DISABLE_INLINE, module.inliningDisabled)
        module.languageVersion?.let { languageVersion ->
            configuration.languageVersionSettings =
                    LanguageVersionSettingsImpl(languageVersion, LanguageVersionSettingsImpl.DEFAULT.apiVersion)
        }

        configuration.put(JSConfigurationKeys.LIBRARIES, JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST + dependencies)
        configuration.put(JSConfigurationKeys.FRIEND_PATHS, friends)

        configuration.put(CommonConfigurationKeys.MODULE_NAME, module.name.removeSuffix(OLD_MODULE_SUFFIX))
        configuration.put(JSConfigurationKeys.MODULE_KIND, module.moduleKind)
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.v5)

        configuration.put(JSConfigurationKeys.SOURCE_MAP, generateSourceMap)

        val hasFilesToRecompile = module.hasFilesToRecompile
        configuration.put(JSConfigurationKeys.META_INFO, multiModule)
        configuration.put(JSConfigurationKeys.SERIALIZE_FRAGMENTS, hasFilesToRecompile)
        configuration.put(JSConfigurationKeys.SOURCE_MAP, hasFilesToRecompile)

        if (additionalMetadata != null) {
            val metadata = PackagesWithHeaderMetadata(
                    FileUtil.loadFileBytes(additionalMetadata.first),
                    additionalMetadata.second.map { FileUtil.loadFileBytes(it) })
            configuration.put(JSConfigurationKeys.FALLBACK_METADATA, metadata)
        }

        if (typedArraysEnabled) {
            configuration.put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, true)
        }

        return JsConfig(project, configuration)
    }

    private fun minifyAndRun(
            workDir: File, allJsFiles: List<String>, generatedJsFiles: List<Pair<String, TestModule>>,
            expectedResult: String, testModuleName: String, testPackage: String?, testFunction: String, withModuleSystem: Boolean,
            minificationThresholdChecker: (Int) -> Unit
    ) {
        val kotlinJsLib = DIST_DIR_JS_PATH + "kotlin.js"
        val kotlinTestJsLib = DIST_DIR_JS_PATH + "kotlin-test.js"
        val kotlinJsLibOutput = File(workDir, "kotlin.min.js").path
        val kotlinTestJsLibOutput = File(workDir, "kotlin-test.min.js").path

        val kotlinJsInputFile = InputFile(kotlinJsLib, kotlinJsLibOutput, "kotlin")
        val kotlinTestJsInputFile = InputFile(kotlinTestJsLib, kotlinTestJsLibOutput, "kotlin-test")

        val filesToMinify = generatedJsFiles.associate { (fileName, module) ->
            val inputFileName = File(fileName).nameWithoutExtension
            fileName to InputFile(fileName, File(workDir, inputFileName + ".min.js").absolutePath, module.name)
        }

        val testFunctionFqn = testModuleName + (if (testPackage.isNullOrEmpty()) "" else ".$testPackage") + ".$testFunction"
        val additionalReachableNodes = setOf(
                testFunctionFqn, "kotlin.kotlin.io.BufferedOutput", "kotlin.kotlin.io.output.flush",
                "kotlin.kotlin.io.output.buffer", "kotlin-test.kotlin.test.overrideAsserter_wbnzx$"
        )
        val allFilesToMinify = filesToMinify.values + kotlinJsInputFile + kotlinTestJsInputFile
        val dceResult = DeadCodeElimination.run(allFilesToMinify, additionalReachableNodes) { }

        val reachableNodes = dceResult.reachableNodes
        minificationThresholdChecker(reachableNodes.size)

        val runList = mutableListOf<String>()
        runList += kotlinJsLibOutput
        runList += kotlinTestJsLibOutput
        runList += TEST_DATA_DIR_PATH + "nashorn-polyfills.js"
        runList += allJsFiles.map { filesToMinify[it]?.outputPath ?: it }

        val result = engineForMinifier.runAndRestoreContext {
            runList.forEach(this::loadFile)
            overrideAsserter()
            eval(NashornJsTestChecker.SETUP_KOTLIN_OUTPUT)
            runTestFunction(testModuleName, testPackage, testFunction, withModuleSystem)
        }
        TestCase.assertEquals(expectedResult, result)
    }

    private inner class TestFileFactoryImpl : TestFileFactory<TestModule, TestFile>, Closeable {
        var testPackage: String? = null
        val tmpDir = KotlinTestUtils.tmpDir("js-tests")
        val defaultModule = TestModule(TEST_MODULE, emptyList(), emptyList())

        override fun createFile(module: TestModule?, fileName: String, text: String, directives: Map<String, String>): TestFile? {
            val currentModule = module ?: defaultModule

            val ktFile = KtPsiFactory(project).createFile(text)
            val boxFunction = ktFile.declarations.find { it is KtNamedFunction && it.name == TEST_FUNCTION  }
            if (boxFunction != null) {
                testPackage = ktFile.packageFqName.asString()
                if (testPackage?.isEmpty() ?: false) {
                    testPackage = null
                }
            }

            val moduleKindMatcher = MODULE_KIND_PATTERN.matcher(text)
            if (moduleKindMatcher.find()) {
                currentModule.moduleKind = ModuleKind.valueOf(moduleKindMatcher.group(1))
            }

            if (NO_INLINE_PATTERN.matcher(text).find()) {
                currentModule.inliningDisabled = true
            }

            val temporaryFile = File(tmpDir, "${currentModule.name}/$fileName")
            KotlinTestUtils.mkdirs(temporaryFile.parentFile)
            temporaryFile.writeText(text, Charsets.UTF_8)

            val version = InTextDirectivesUtils.findStringWithPrefixes(text, "// LANGUAGE_VERSION:")
            if (version != null) {
                assert(currentModule.languageVersion == null) { "Should not specify LANGUAGE_VERSION twice" }
                currentModule.languageVersion = LanguageVersion.fromVersionString(version)
            }

            return TestFile(temporaryFile.absolutePath, currentModule, recompile = RECOMPILE_PATTERN.matcher(text).find())
        }

        override fun createModule(name: String, dependencies: List<String>, friends: List<String>): TestModule? {
            return TestModule(name, dependencies, friends)
        }

        override fun close() {
            FileUtil.delete(tmpDir)
        }
    }

    private class TestFile(val fileName: String, val module: TestModule, val recompile: Boolean) {
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
        var languageVersion: LanguageVersion? = null

        val hasFilesToRecompile get() = files.any { it.recompile }
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(testRootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)
    }

    companion object {
        const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"
        const val DIST_DIR_JS_PATH = "dist/js/"

        private val COMMON_FILES_NAME = "_common"
        private val COMMON_FILES_DIR = "_commonFiles/"
        private val MODULE_EMULATION_FILE = TEST_DATA_DIR_PATH + "/moduleEmulation.js"

        private val MODULE_KIND_PATTERN = Pattern.compile("^// *MODULE_KIND: *(.+)$", Pattern.MULTILINE)
        private val NO_MODULE_SYSTEM_PATTERN = Pattern.compile("^// *NO_JS_MODULE_SYSTEM", Pattern.MULTILINE)
        private val NO_INLINE_PATTERN = Pattern.compile("^// *NO_INLINE *$", Pattern.MULTILINE)
        private val SKIP_NODE_JS = Pattern.compile("^// *SKIP_NODE_JS *$", Pattern.MULTILINE)
        private val SKIP_MINIFICATION = Pattern.compile("^// *SKIP_MINIFICATION *$", Pattern.MULTILINE)
        private val EXPECTED_REACHABLE_NODES_DIRECTIVE = "EXPECTED_REACHABLE_NODES"
        private val EXPECTED_REACHABLE_NODES = Pattern.compile("^// *$EXPECTED_REACHABLE_NODES_DIRECTIVE: *([0-9]+) *$", Pattern.MULTILINE)
        private val RECOMPILE_PATTERN = Pattern.compile("^// *RECOMPILE *$", Pattern.MULTILINE)
        private val AST_EXTENSION = "jsast"
        private val METADATA_EXTENSION = "jsmeta"
        private val HEADER_FILE = "header.$METADATA_EXTENSION"

        private val TEST_MODULE = "JS_TESTS"
        private val DEFAULT_MODULE = "main"
        private val TEST_FUNCTION = "box"
        private val OLD_MODULE_SUFFIX = "-old"

        const val KOTLIN_TEST_INTERNAL = "\$kotlin_test_internal\$"

        private val engineForMinifier = createScriptEngine()
    }
}
