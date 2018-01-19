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

import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import junit.framework.TestCase
import org.jetbrains.kotlin.checkers.parseLanguageVersionSettings
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.output.outputUtils.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.incremental.js.IncrementalDataProviderImpl
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.incremental.js.TranslationResultValue
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.js.dce.DeadCodeElimination
import org.jetbrains.kotlin.js.dce.InputFile
import org.jetbrains.kotlin.js.dce.InputResource
import org.jetbrains.kotlin.js.facade.*
import org.jetbrains.kotlin.js.parser.parse
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapError
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapLocationRemapper
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapParser
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSuccess
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.js.test.utils.*
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.serialization.DebugProtoBuf
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestUtils.TestFileFactory
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadata
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.*
import java.nio.charset.Charset
import java.util.regex.Pattern

abstract class BasicBoxTest(
        protected val pathToTestDir: String,
        testGroupOutputDirPrefix: String,
        pathToRootOutputDir: String = BasicBoxTest.TEST_DATA_DIR_PATH,
        private val typedArraysEnabled: Boolean = true,
        private val generateSourceMap: Boolean = false,
        private val generateNodeJsRunner: Boolean = true
) : KotlinTestWithEnvironment() {
    val additionalCommonFileDirectories = mutableListOf<String>()

    private val testGroupOutputDirForCompilation = File(pathToRootOutputDir + "out/" + testGroupOutputDirPrefix)
    private val testGroupOutputDirForMinification = File(pathToRootOutputDir + "out-min/" + testGroupOutputDirPrefix)

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
            val inputFiles = KotlinTestUtils.createTestFiles(file.name, fileContent, testFactory, true)
            val modules = inputFiles
                    .map { it.module }.distinct()
                    .map { it.name to it }.toMap()

            val orderedModules = DFS.topologicalOrder(modules.values) { module -> module.dependencies.mapNotNull { modules[it] } }

            val generatedJsFiles = orderedModules.asReversed().mapNotNull { module ->
                val dependencies = module.dependencies.map { modules[it]?.outputFileName(outputDir) + ".meta.js" }
                val friends = module.friends.map { modules[it]?.outputFileName(outputDir) + ".meta.js" }

                val outputFileName = module.outputFileName(outputDir) + ".js"
                generateJavaScriptFile(file.parent, module, outputFileName, dependencies, friends, modules.size > 1,
                                       ENABLE_MULTIPLATFORM.matcher(fileContent).find(),
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
                    .map { inputJsFile ->
                        val sourceFile = File(inputJsFile.fileName)
                        val targetFile = File(outputDir, inputJsFile.module.outputFileSimpleName() + "-js-" + sourceFile.name)
                        FileUtil.copy(File(inputJsFile.fileName), targetFile)
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

                val outputDirForMinification = getOutputDir(file, testGroupOutputDirForMinification)
                minifyAndRun(
                        workDir = File(outputDirForMinification, file.nameWithoutExtension),
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

    protected fun getOutputDir(file: File, testGroupOutputDir: File = testGroupOutputDirForCompilation): File {
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
            friends: List<String>,
            multiModule: Boolean,
            multiplatform: Boolean,
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
        val allSourceFiles = (testFiles + additionalFiles).map(::File)
        val psiFiles = createPsiFiles(allSourceFiles.sortedBy { it.canonicalPath }.map { it.canonicalPath })

        val sourceDirs = (testFiles + additionalFiles).map { File(it).parent }.distinct()
        val config = createConfig(sourceDirs, module, dependencies, friends, multiModule, multiplatform, incrementalData = null)
        val outputFile = File(outputFileName)

        val incrementalData = IncrementalData()
        translateFiles(psiFiles.map(TranslationUnit::SourceFile), outputFile, config, outputPrefixFile, outputPostfixFile,
                       mainCallParameters, incrementalData)

        if (module.hasFilesToRecompile) {
            checkIncrementalCompilation(sourceDirs, module, kotlinFiles, dependencies, friends, multiModule, multiplatform, outputFile,
                                        outputPrefixFile, outputPostfixFile, mainCallParameters, incrementalData)
        }
    }

    private fun checkIncrementalCompilation(
            sourceDirs: List<String>,
            module: TestModule,
            kotlinFiles: List<TestFile>,
            dependencies: List<String>,
            friends: List<String>,
            multiModule: Boolean,
            multiplatform: Boolean,
            outputFile: File,
            outputPrefixFile: File?,
            outputPostfixFile: File?,
            mainCallParameters: MainCallParameters,
            incrementalData: IncrementalData
    ) {
        val sourceToTranslationUnit = hashMapOf<File, TranslationUnit>()
        for (testFile in kotlinFiles) {
            if (testFile.recompile) {
                val sourceFile = File(testFile.fileName)
                incrementalData.translatedFiles.remove(sourceFile)
                sourceToTranslationUnit[sourceFile] = TranslationUnit.SourceFile(createPsiFile(testFile.fileName))
            }
        }
        for ((sourceFile, data) in incrementalData.translatedFiles) {
            sourceToTranslationUnit[sourceFile] = TranslationUnit.BinaryAst(data.binaryAst)
        }
        val translationUnits = sourceToTranslationUnit.keys
                .sortedBy { it.canonicalPath }
                .map { sourceToTranslationUnit[it]!! }

        val recompiledConfig = createConfig(sourceDirs, module, dependencies, friends, multiModule, multiplatform, incrementalData)
        val recompiledOutputFile = File(outputFile.parentFile, outputFile.nameWithoutExtension + "-recompiled.js")

        translateFiles(translationUnits, recompiledOutputFile, recompiledConfig, outputPrefixFile, outputPostfixFile,
                       mainCallParameters, incrementalData)

        val originalOutput = FileUtil.loadFile(outputFile)
        val recompiledOutput = removeRecompiledSuffix(FileUtil.loadFile(recompiledOutputFile))
        assertEquals("Output file changed after recompilation", originalOutput, recompiledOutput)

        val originalSourceMap = FileUtil.loadFile(File(outputFile.parentFile, outputFile.name + ".map"))
        val recompiledSourceMap = removeRecompiledSuffix(
                FileUtil.loadFile(File(recompiledOutputFile.parentFile, recompiledOutputFile.name + ".map")))
        if (originalSourceMap != recompiledSourceMap) {
            val originalSourceMapParse = SourceMapParser.parse(StringReader(originalSourceMap))
            val recompiledSourceMapParse = SourceMapParser.parse(StringReader(recompiledSourceMap))
            if (originalSourceMapParse is SourceMapSuccess && recompiledSourceMapParse is SourceMapSuccess) {
                assertEquals("Source map file changed after recompilation",
                             originalSourceMapParse.toDebugString(),
                             recompiledSourceMapParse.toDebugString())
            }
            assertEquals("Source map file changed after recompilation", originalSourceMap, recompiledSourceMap)
        }

        if (multiModule) {
            val originalMetadata = FileUtil.loadFile(File(outputFile.parentFile, outputFile.nameWithoutExtension + ".meta.js"))
            val recompiledMetadata = removeRecompiledSuffix(
                    FileUtil.loadFile(File(recompiledOutputFile.parentFile, recompiledOutputFile.nameWithoutExtension + ".meta.js")))
            assertEquals("Metadata file changed after recompilation",
                                  metadataAsString(originalMetadata, module.name),
                                  metadataAsString(recompiledMetadata, module.name))
        }
    }

    private fun SourceMapSuccess.toDebugString(): String {
        val out = ByteArrayOutputStream()
        PrintStream(out).use { value.debug(it) }
        return String(out.toByteArray(), Charset.forName("UTF-8"))
    }

    private fun metadataAsString(metadata: String, moduleName: String): String {
        val containers = mutableListOf<KotlinJavascriptMetadata>()
        KotlinJavascriptMetadataUtils.parseMetadata(metadata, containers)
        val metadataParts = KotlinJavascriptSerializationUtil.readModuleAsProto(containers.single().body, moduleName).data.body
        return metadataParts.joinToString("-----\n") {
            val binary = it.toByteArray()
            DebugProtoBuf.PackageFragment.parseFrom(binary, JsSerializerProtocol.extensionRegistry).toString()
        }
    }

    private fun removeRecompiledSuffix(text: String): String = text.replace("-recompiled.js", ".js")

    class IncrementalData(var header: ByteArray? = null, val translatedFiles: MutableMap<File, TranslationResultValue> = hashMapOf())

    private fun translateFiles(
            units: List<TranslationUnit>,
            outputFile: File,
            config: JsConfig,
            outputPrefixFile: File?,
            outputPostfixFile: File?,
            mainCallParameters: MainCallParameters,
            incrementalData: IncrementalData
    ) {
        val translator = K2JSTranslator(config)
        val translationResult = translator.translateUnits(ExceptionThrowingReporter, units, mainCallParameters)

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

        config.configuration[JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER]?.let {
            val incrementalService = it as IncrementalResultsConsumerImpl

            for ((srcFile, data) in incrementalService.packageParts) {
                incrementalData.translatedFiles[srcFile] = data
            }

            incrementalData.header = incrementalService.headerMetadata
        }

        processJsProgram(translationResult.program, units.filterIsInstance<TranslationUnit.SourceFile>().map { it.file })
        checkSourceMap(outputFile, translationResult.program)
    }

    private fun processJsProgram(program: JsProgram, psiFiles: List<KtFile>) {
        psiFiles.asSequence()
                .map { it.text }
                .forEach { DirectiveTestUtils.processDirectives(program, it) }
        program.verifyAst()
    }

    private fun checkSourceMap(outputFile: File, program: JsProgram) {
        val generatedProgram = JsProgram()
        generatedProgram.globalBlock.statements += program.globalBlock.statements.map { it.deepCopy() }
        generatedProgram.accept(object : RecursiveJsVisitor() {
            override fun visitObjectLiteral(x: JsObjectLiteral) {
                super.visitObjectLiteral(x)
                x.isMultiline = false
            }
            override fun visitVars(x: JsVars) {
                x.isMultiline = false
                super.visitVars(x)
            }
        })
        removeLocationFromBlocks(generatedProgram)
        generatedProgram.accept(AmbiguousAstSourcePropagation())

        val output = TextOutputImpl()
        val pathResolver = SourceFilePathResolver(mutableListOf(File(".")), null)
        val sourceMapBuilder = SourceMap3Builder(outputFile, output, "")
        generatedProgram.accept(JsToStringGenerationVisitor(
                output, SourceMapBuilderConsumer(File("."), sourceMapBuilder, pathResolver, false, false)))
        val code = output.toString()
        val generatedSourceMap = sourceMapBuilder.build()

        val codeWithLines = generatedProgram.toStringWithLineNumbers()

        val parsedProgram = JsProgram()
        parsedProgram.globalBlock.statements += parse(code, ThrowExceptionOnErrorReporter, parsedProgram.scope, outputFile.path).orEmpty()
        removeLocationFromBlocks(parsedProgram)
        val sourceMapParseResult = SourceMapParser.parse(StringReader(generatedSourceMap))
        val sourceMap = when (sourceMapParseResult) {
            is SourceMapSuccess -> sourceMapParseResult.value
            is SourceMapError -> error("Could not parse source map: ${sourceMapParseResult.message}")
        }

        val remapper = SourceMapLocationRemapper(sourceMap)
        remapper.remap(parsedProgram)

        val codeWithRemappedLines = parsedProgram.toStringWithLineNumbers()

        TestCase.assertEquals(codeWithLines, codeWithRemappedLines)
    }

    private fun removeLocationFromBlocks(program: JsProgram) {
        program.globalBlock.accept(object : RecursiveJsVisitor() {
            override fun visitBlock(x: JsBlock) {
                super.visitBlock(x)
                x.source = null
            }
        })
    }

    private fun createPsiFile(fileName: String): KtFile {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        return psiManager.findFile(fileSystem.findFileByPath(fileName)!!) as KtFile
    }

    private fun createPsiFiles(fileNames: List<String>): List<KtFile> = fileNames.map(this::createPsiFile)

    private fun createConfig(
            sourceDirs: List<String>,module: TestModule, dependencies: List<String>, friends: List<String>,
            multiModule: Boolean, multiplatform: Boolean, incrementalData: IncrementalData?
    ): JsConfig {
        val configuration = environment.configuration.copy()

        configuration.put(CommonConfigurationKeys.DISABLE_INLINE, module.inliningDisabled)
        module.languageVersionSettings?.let { languageVersionSettings ->
            configuration.languageVersionSettings = languageVersionSettings
        }

        configuration.put(JSConfigurationKeys.LIBRARIES, JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST + dependencies)
        configuration.put(JSConfigurationKeys.FRIEND_PATHS, friends)

        configuration.put(CommonConfigurationKeys.MODULE_NAME, module.name.removeSuffix(OLD_MODULE_SUFFIX))
        configuration.put(JSConfigurationKeys.MODULE_KIND, module.moduleKind)
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.v5)

        val hasFilesToRecompile = module.hasFilesToRecompile
        configuration.put(JSConfigurationKeys.META_INFO, multiModule)
        if (hasFilesToRecompile) {
            val header = incrementalData?.header
            if (header != null) {
                configuration.put(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER,
                                  IncrementalDataProviderImpl(header, incrementalData.translatedFiles))
            }

            configuration.put(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER, IncrementalResultsConsumerImpl())
        }
        configuration.put(JSConfigurationKeys.SOURCE_MAP, true)
        configuration.put(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, sourceDirs)
        configuration.put(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, module.sourceMapSourceEmbedding)

        configuration.put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, typedArraysEnabled)

        if (multiplatform) {
            val defaultLanguageVersionSettings = configuration.languageVersionSettings
            configuration.languageVersionSettings = object : LanguageVersionSettings by defaultLanguageVersionSettings {
                override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State =
                        if (feature == LanguageFeature.MultiPlatformProjects) LanguageFeature.State.ENABLED
                        else defaultLanguageVersionSettings.getFeatureSupport(feature)
            }
        }

        return JsConfig(project, configuration, METADATA_CACHE, (JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST).toSet())
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

        val kotlinJsInputFile = InputFile(InputResource.file(kotlinJsLib), null, kotlinJsLibOutput, "kotlin")
        val kotlinTestJsInputFile = InputFile(InputResource.file(kotlinTestJsLib), null, kotlinTestJsLibOutput, "kotlin-test")

        val filesToMinify = generatedJsFiles.associate { (fileName, module) ->
            val inputFileName = File(fileName).nameWithoutExtension
            fileName to InputFile(InputResource.file(fileName), null, File(workDir, inputFileName + ".min.js").absolutePath, module.name)
        }

        val testFunctionFqn = testModuleName + (if (testPackage.isNullOrEmpty()) "" else ".$testPackage") + ".$testFunction"
        val additionalReachableNodes = setOf(
                testFunctionFqn, "kotlin.kotlin.io.BufferedOutput", "kotlin.kotlin.io.output.flush",
                "kotlin.kotlin.io.output.buffer", "kotlin-test.kotlin.test.overrideAsserter_wbnzx$",
                "kotlin-test.kotlin.test.DefaultAsserter"
        )
        val allFilesToMinify = filesToMinify.values + kotlinJsInputFile + kotlinTestJsInputFile
        val dceResult = DeadCodeElimination.run(allFilesToMinify, additionalReachableNodes) { _, _ -> }

        val reachableNodes = dceResult.reachableNodes
        minificationThresholdChecker(reachableNodes.count { it.reachable })

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
                if (testPackage?.isEmpty() == true) {
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

            val old = currentModule.languageVersionSettings
            val new = parseLanguageVersionSettings(directives)
            assert(old == null || old == new) { "Should not specify language version settings twice:\n$old\n$new" }
            currentModule.languageVersionSettings = new

            SOURCE_MAP_SOURCE_EMBEDDING.find(text)?.let { match ->
                currentModule.sourceMapSourceEmbedding = SourceMapSourceEmbedding.valueOf(match.groupValues[1])
            }

            return TestFile(temporaryFile.absolutePath, currentModule, recompile = RECOMPILE_PATTERN.matcher(text).find())
        }

        override fun createModule(name: String, dependencies: List<String>, friends: List<String>) =
                TestModule(name, dependencies, friends)

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
        var languageVersionSettings: LanguageVersionSettings? = null
        var sourceMapSourceEmbedding = SourceMapSourceEmbedding.NEVER

        val hasFilesToRecompile get() = files.any { it.recompile }
    }

    override fun createEnvironment() =
            KotlinCoreEnvironment.createForTests(testRootDisposable, CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES)

    companion object {
        val METADATA_CACHE = (JsConfig.JS_STDLIB.asSequence() + JsConfig.JS_KOTLIN_TEST)
                .flatMap {
                    KotlinJavascriptMetadataUtils
                            .loadMetadata(it).asSequence()
                            .map { KotlinJavascriptSerializationUtil.readModuleAsProto(it.body, it.moduleName) }
                }
                .toList()

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
        private val SOURCE_MAP_SOURCE_EMBEDDING = Regex("^// *SOURCE_MAP_EMBED_SOURCES: ([A-Z]+)*\$", RegexOption.MULTILINE)
        private val ENABLE_MULTIPLATFORM = Pattern.compile("^// *MULTIPLATFORM *$", Pattern.MULTILINE)

        val TEST_MODULE = "JS_TESTS"
        private val DEFAULT_MODULE = "main"
        private val TEST_FUNCTION = "box"
        private val OLD_MODULE_SUFFIX = "-old"

        const val KOTLIN_TEST_INTERNAL = "\$kotlin_test_internal\$"

        private val engineForMinifier = createScriptEngine()
    }
}
