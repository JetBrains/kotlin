/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import junit.framework.TestCase
import org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettings
import org.jetbrains.kotlin.checkers.parseLanguageVersionSettings
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.incremental.js.IncrementalDataProviderImpl
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumerImpl
import org.jetbrains.kotlin.incremental.js.TranslationResultValue
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.ic.ICCache
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationResult
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapParser
import org.jetbrains.kotlin.js.parser.sourcemaps.SourceMapSuccess
import org.jetbrains.kotlin.js.test.utils.ExceptionThrowingReporter
import org.jetbrains.kotlin.js.testNew.*
import org.jetbrains.kotlin.js.testNew.handlers.JsAstHandler
import org.jetbrains.kotlin.js.testNew.handlers.JsMinifierRunner
import org.jetbrains.kotlin.js.testNew.handlers.JsSourceMapHandler
import org.jetbrains.kotlin.js.testNew.handlers.NodeJsGeneratorHandler
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.js.test.engines.ExternalTool
import org.jetbrains.kotlin.js.sourceMap.SourceMapBuilderConsumer
import org.jetbrains.kotlin.js.test.utils.*
import org.jetbrains.kotlin.js.util.TextOutputImpl
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.metadata.DebugProtoBuf
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.serialization.js.JsModuleDescriptor
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadata
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import org.junit.Assert
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.File
import java.io.PrintStream
import java.lang.Boolean.getBoolean
import java.nio.charset.Charset
import java.util.regex.Pattern

abstract class BasicBoxTest(
    protected val pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    private val typedArraysEnabled: Boolean = true,
    private val generateSourceMap: Boolean = false,
    private val generateNodeJsRunner: Boolean = true,
    private val targetBackend: TargetBackend = TargetBackend.JS
) : KotlinTestWithEnvironment() {
    private val pathToRootOutputDir = System.getProperty("kotlin.js.test.root.out.dir") ?: error("'kotlin.js.test.root.out.dir' is not set")
    private val testGroupOutputDirForCompilation = File(pathToRootOutputDir + "out/" + testGroupOutputDirPrefix)
    private val testGroupOutputDirForMinification = File(pathToRootOutputDir + "out-min/" + testGroupOutputDirPrefix)
    private val testGroupOutputDirForPir = File(pathToRootOutputDir + "out-pir/" + testGroupOutputDirPrefix)

    protected open fun getOutputPrefixFile(testFilePath: String): File? = null
    protected open fun getOutputPostfixFile(testFilePath: String): File? = null

    protected open val runMinifierByDefault: Boolean = false
    protected open val skipMinification = getBoolean("kotlin.js.skipMinificationTest")

    protected open val skipRegularMode: Boolean = false
    protected open val runIrDce: Boolean = false
    protected open val runIrPir: Boolean = false

    protected open val incrementalCompilationChecksEnabled = true

    protected open val testChecker get() = if (runTestInNashorn) NashornJsTestChecker else V8JsTestChecker

    protected val logger = KotlinJsTestLogger()


    fun doTest(filePath: String) {
        doTestWithIgnoringByFailFile(filePath)
    }

    fun doTestWithIgnoringByFailFile(filePath: String) {
        val failFile = File("$filePath.fail")
        try {
            doTest(filePath, "OK", MainCallParameters.noCall())
        } catch (e: Throwable) {
            if (failFile.exists()) {
                KotlinTestUtils.assertEqualsToFile(failFile, e.message ?: "")
            } else {
                throw e
            }
        }
    }

    open fun doTest(filePath: String, expectedResult: String, mainCallParameters: MainCallParameters) {
        val file = File(filePath)

        @Suppress("NAME_SHADOWING")
        val filePath = file.absolutePath

        logger.logFile("Test file", file)

        val outputDir = getOutputDir(file)
        val dceOutputDir = getOutputDir(file, testGroupOutputDirForMinification)
        val pirOutputDir = getOutputDir(file, testGroupOutputDirForPir)
        val fileContent = KtTestUtil.doLoadFile(file)

        val needsFullIrRuntime = KJS_WITH_FULL_RUNTIME.matcher(fileContent).find() || WITH_RUNTIME.matcher(fileContent).find() || WITH_STDLIB.matcher(fileContent).find()


        val actualMainCallParameters = if (CALL_MAIN_PATTERN.matcher(fileContent).find())
            MainCallParameters.mainWithArguments(listOf())
        else
            mainCallParameters

        val outputPrefixFile = getOutputPrefixFile(filePath)
        val outputPostfixFile = getOutputPostfixFile(filePath)

        val runPlainBoxFunction = RUN_PLAIN_BOX_FUNCTION.matcher(fileContent).find()
        val inferMainModule = INFER_MAIN_MODULE.matcher(fileContent).find()
        val expectActualLinker = EXPECT_ACTUAL_LINKER.matcher(fileContent).find()
        val errorPolicyMatcher = ERROR_POLICY_PATTERN.matcher(fileContent)
        val errorPolicy =
            if (errorPolicyMatcher.find()) ErrorTolerancePolicy.resolvePolicy(errorPolicyMatcher.group(1)) else ErrorTolerancePolicy.DEFAULT

        val skipDceDriven = SKIP_DCE_DRIVEN.matcher(fileContent).find()
        val esModules = ES_MODULES.matcher(fileContent).find()

        val splitPerModule = SPLIT_PER_MODULE.matcher(fileContent).find()
        val splitPerFile = SPLIT_PER_FILE.matcher(fileContent).find()

        val granularity = when {
            splitPerModule -> JsGenerationGranularity.PER_MODULE
            splitPerFile -> JsGenerationGranularity.PER_FILE
            else -> JsGenerationGranularity.WHOLE_PROGRAM
        }

        val skipMangleVerification = SKIP_MANGLE_VERIFICATION.matcher(fileContent).find()

        val safeExternalBoolean = SAFE_EXTERNAL_BOOLEAN.matcher(fileContent).find()
        val safeExternalBooleanDiagnosticMatcher = SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC.matcher(fileContent)

        val safeExternalBooleanDiagnostic =
            if (safeExternalBooleanDiagnosticMatcher.find())
                RuntimeDiagnostic.resolve(safeExternalBooleanDiagnosticMatcher.group(1))
            else null

        val skipIcChecks = targetBackend == TargetBackend.JS_IR && SKIP_IR_INCREMENTAL_CHECKS.matcher(fileContent).find()

        TestFileFactoryImpl().use { testFactory ->
            val inputFiles = TestFiles.createTestFiles(
                file.name,
                fileContent,
                testFactory,
                true
            )
            val modules = inputFiles
                .map { it.module }.distinct()
                .map { it.name to it }.toMap()

            fun TestModule.allTransitiveDependencies(): Set<String> {
                return dependenciesSymbols.toSet() + dependenciesSymbols.flatMap { modules[it]!!.allTransitiveDependencies() }
            }

            val checkIC = !skipIcChecks && modules.any { it.value.hasFilesToRecompile }

            val orderedModules = DFS.topologicalOrder(modules.values) { module -> module.dependenciesSymbols.mapNotNull { modules[it] } }

            val testPackage = if (runPlainBoxFunction) null else testFactory.testPackage

            val testFunction = TEST_FUNCTION

            val mainModuleName = when {
                inferMainModule -> orderedModules.last().name
                TEST_MODULE in modules -> TEST_MODULE
                else -> DEFAULT_MODULE
            }
            val testModuleName = if (runPlainBoxFunction) null else mainModuleName
            val mainModule = modules[mainModuleName] ?: error("No module with name \"$mainModuleName\"")
            val icCache = mutableMapOf<String, TestModuleCache>()

            if (esModules) {
                modules.forEach { _, m -> m.moduleKind = ModuleKind.ES }
            }

            val customTestModule: String? = inputFiles.find { it.testEntryEsModule }?.let { File(it.fileName).readText() }

            val generatedJsFiles = orderedModules.asReversed().mapNotNull { module ->
                val dependencies = module.dependenciesSymbols.map { modules[it]?.outputFileName(outputDir) + ".meta.js" }
                val allDependencies = module.allTransitiveDependencies().map { modules[it]?.outputFileName(outputDir) + ".meta.js" }
                val friends = module.friendsSymbols.map { modules[it]?.outputFileName(outputDir) + ".meta.js" }

                val outputFileName = module.outputFileName(outputDir) + ".js"
                val dceOutputFileName = module.outputFileName(dceOutputDir) + ".js"
                val pirOutputFileName = module.outputFileName(pirOutputDir) + ".js"
                val abiVersion = module.abiVersion
                val isMainModule = mainModuleName == module.name

                logger.logFile("Output JS", File(outputFileName))
                generateJavaScriptFile(
                    testFactory.tmpDir,
                    file.parent,
                    module,
                    outputFileName,
                    dceOutputFileName,
                    pirOutputFileName,
                    dependencies,
                    allDependencies,
                    friends,
                    modules.size > 1,
                    !SKIP_SOURCEMAP_REMAPPING.matcher(fileContent).find(),
                    outputPrefixFile,
                    outputPostfixFile,
                    actualMainCallParameters,
                    testPackage,
                    testFunction,
                    needsFullIrRuntime,
                    isMainModule,
                    expectActualLinker,
                    skipDceDriven,
                    esModules,
                    granularity,
                    errorPolicy,
                    propertyLazyInitialization = true,
                    safeExternalBoolean,
                    safeExternalBooleanDiagnostic,
                    skipMangleVerification,
                    abiVersion,
                    checkIC,
                    icCache,
                    customTestModule,
                )

                when {
                    module.name.endsWith(OLD_MODULE_SUFFIX) -> null
                    // JS_IR generates single js file for all modules (apart from runtime).
                    // TODO: Split and refactor test runner for JS_IR
                    targetBackend in listOf(TargetBackend.JS_IR, TargetBackend.JS_IR_ES6) && !isMainModule -> null
                    else -> Pair(outputFileName, module)
                }
            }

            val commonFiles = JsAdditionalSourceProvider.getAdditionalJsFiles(file.parent).map { it.absolutePath }
            val inputJsFilesBefore = mutableListOf<String>()
            val inputJsFilesAfter = mutableListOf<String>()

            fun copyInputJsFile(inputJsFile: TestFile): String {
                val sourceFile = File(inputJsFile.fileName)
                val targetFile = File(outputDir, inputJsFile.module.outputFileSimpleName() + "-js-" + sourceFile.name)
                FileUtil.copy(File(inputJsFile.fileName), targetFile)
                return targetFile.absolutePath
            }

            inputFiles.forEach {
                val name = it.fileName
                when {
                    name.endsWith("__after.js") ->
                        inputJsFilesAfter += copyInputJsFile(it)

                    name.endsWith(".js") ->
                        inputJsFilesBefore += copyInputJsFile(it)
                }
            }

            val additionalFiles = mutableListOf<String>()
            val moduleEmulationFiles = mutableListOf<String>()

            val moduleKindMatcher = MODULE_KIND_PATTERN.matcher(fileContent)
            val moduleKind = if (moduleKindMatcher.find()) ModuleKind.valueOf(moduleKindMatcher.group(1)) else ModuleKind.PLAIN

            val withModuleSystem = moduleKind != ModuleKind.PLAIN && !NO_MODULE_SYSTEM_PATTERN.matcher(fileContent).find()

            if (withModuleSystem) {
                moduleEmulationFiles += File(MODULE_EMULATION_FILE).absolutePath
            }

            val additionalJsFile = filePath.removeSuffix("." + KotlinFileType.EXTENSION) + JavaScript.DOT_EXTENSION
            if (File(additionalJsFile).exists()) {
                additionalFiles += additionalJsFile
            }

            if (esModules) {
                val additionalMainJsFile =
                    (filePath.removeSuffix("." + KotlinFileType.EXTENSION) + "__main.js").takeIf { File(it).exists() }

                val maybeAdditionalMjsFile: String? =
                    (filePath.removeSuffix("." + KotlinFileType.EXTENSION) + ".mjs")
                        .takeIf { File(it).exists() }

                val allMjsFiles: List<String> =
                    inputFiles.filter { it.fileName.endsWith(".mjs") }.map { it.fileName } + listOfNotNull(maybeAdditionalMjsFile)

                val allNonEsModuleFiles: List<String> =
                    additionalFiles + inputJsFilesBefore + commonFiles

                fun runIrEsmTests(testOutputDir: File) {
                    val esmOutputDir = testOutputDir.esModulesSubDir

                    // Copy __main file if present
                    if (additionalMainJsFile != null) {
                        val newFileName = File(esmOutputDir, "test.mjs")
                        newFileName.delete()
                        File(additionalMainJsFile).copyTo(newFileName)
                    }

                    // Copy all .mjs files into generated directory
                    allMjsFiles.forEach { mjsFile ->
                        val outFile = File(esmOutputDir, File(mjsFile).name)
                        File(mjsFile).copyTo(outFile)
                    }

                    val perFileEsModuleFile = "$esmOutputDir/test.mjs"
                    v8tool.run(*allNonEsModuleFiles.toTypedArray(), perFileEsModuleFile, *inputJsFilesAfter.toTypedArray())
                }

                fun File.getTestDir(): File =
                    File(generatedJsFiles.single().first.replace(outputDir.absolutePath, this.absolutePath))

                if (!skipRegularMode) {
                    runIrEsmTests(outputDir.getTestDir())

                    if (runIrDce) {
                        runIrEsmTests(dceOutputDir.getTestDir())
                    }
                }

                if (runIrPir && !skipDceDriven) {
                    runIrEsmTests(pirOutputDir.getTestDir())
                }
            }

            if (esModules)
                return

            // Old systems tests

            additionalFiles.addAll(0, moduleEmulationFiles)

            val additionalMainFiles = mutableListOf<String>()
            val additionalMainJsFile = filePath.removeSuffix("." + KotlinFileType.EXTENSION) + "__main.js"
            if (File(additionalMainJsFile).exists()) {
                additionalMainFiles += additionalMainJsFile
            }

            val allJsFiles = additionalFiles + inputJsFilesBefore + generatedJsFiles.map { it.first } + commonFiles +
                    additionalMainFiles + inputJsFilesAfter

            val dceAllJsFiles = additionalFiles + inputJsFilesBefore + generatedJsFiles.map {
                it.first.replace(
                    outputDir.absolutePath,
                    dceOutputDir.absolutePath
                )
            } + commonFiles + additionalMainFiles + inputJsFilesAfter

            val pirAllJsFiles = additionalFiles + inputJsFilesBefore + generatedJsFiles.map {
                it.first.replace(
                    outputDir.absolutePath,
                    pirOutputDir.absolutePath
                )
            } + commonFiles + additionalMainFiles + inputJsFilesAfter


            val dontRunGeneratedCode = InTextDirectivesUtils.dontRunGeneratedCode(targetBackend, file)
            if (!dontRunGeneratedCode && generateNodeJsRunner && !SKIP_NODE_JS.matcher(fileContent).find()) {
                val nodeRunnerName = mainModule.outputFileName(outputDir) + ".node.js"
                val ignored = InTextDirectivesUtils.isIgnoredTarget(TargetBackend.JS, file)
                val nodeRunnerText = NodeJsGeneratorHandler.generateNodeRunner(allJsFiles, outputDir, mainModuleName, ignored, testPackage)
                FileUtil.writeToFile(File(nodeRunnerName), nodeRunnerText)
            }

            if (!dontRunGeneratedCode) {
                if (!skipRegularMode) {
                    runGeneratedCode(allJsFiles, testModuleName, testPackage, testFunction, expectedResult, withModuleSystem)

                    if (runIrDce && !checkIC) {
                        runGeneratedCode(dceAllJsFiles, testModuleName, testPackage, testFunction, expectedResult, withModuleSystem)
                    }
                }

                if (runIrPir && !skipDceDriven) {
                    runGeneratedCode(pirAllJsFiles, testModuleName, testPackage, testFunction, expectedResult, withModuleSystem)
                }
            }

            performAdditionalChecks(generatedJsFiles.map { it.first }, outputPrefixFile, outputPostfixFile)
            performAdditionalChecks(file, File(mainModule.outputFileName(outputDir)))
            val expectedReachableNodesMatcher = EXPECTED_REACHABLE_NODES.matcher(fileContent)
            val expectedReachableNodesFound = expectedReachableNodesMatcher.find()

            if (!dontRunGeneratedCode && !skipMinification &&
                (runMinifierByDefault || expectedReachableNodesFound) &&
                !SKIP_MINIFICATION.matcher(fileContent).find()
            ) {
                val outputDirForMinification = getOutputDir(file, testGroupOutputDirForMinification)

                JsMinifierRunner.minifyAndRun(
                    file = file,
                    expectedReachableNodes = expectedReachableNodesMatcher.group(1).toInt(),
                    workDir = File(outputDirForMinification, file.nameWithoutExtension),
                    allJsFiles = allJsFiles,
                    generatedJsFiles = generatedJsFiles.map { it.first to it.second.name },
                    expectedResult = expectedResult,
                    testModuleName = testModuleName,
                    testPackage = testPackage,
                    testFunction = testFunction,
                    withModuleSystem = withModuleSystem,
                ) { expect, actual -> TestCase.assertEquals(expect, actual) }
            }
        }
    }

    protected open fun runGeneratedCode(
        jsFiles: List<String>,
        testModuleName: String?,
        testPackage: String?,
        testFunction: String,
        expectedResult: String,
        withModuleSystem: Boolean
    ) {
        testChecker.check(jsFiles, testModuleName, testPackage, testFunction, expectedResult, withModuleSystem)
    }

    protected open fun performAdditionalChecks(generatedJsFiles: List<String>, outputPrefixFile: File?, outputPostfixFile: File?) {}
    protected open fun performAdditionalChecks(inputFile: File, outputFile: File) {}

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
        tmpDir: File,
        directory: String,
        module: TestModule,
        outputFileName: String,
        dceOutputFileName: String,
        pirOutputFileName: String,
        dependencies: List<String>,
        allDependencies: List<String>,
        friends: List<String>,
        multiModule: Boolean,
        remap: Boolean,
        outputPrefixFile: File?,
        outputPostfixFile: File?,
        mainCallParameters: MainCallParameters,
        testPackage: String?,
        testFunction: String,
        needsFullIrRuntime: Boolean,
        isMainModule: Boolean,
        expectActualLinker: Boolean,
        skipDceDriven: Boolean,
        esModules: Boolean,
        granularity: JsGenerationGranularity,
        errorIgnorancePolicy: ErrorTolerancePolicy,
        propertyLazyInitialization: Boolean,
        safeExternalBoolean: Boolean,
        safeExternalBooleanDiagnostic: RuntimeDiagnostic?,
        skipMangleVerification: Boolean,
        abiVersion: KotlinAbiVersion,
        checkIC: Boolean,
        icCache: MutableMap<String, TestModuleCache>,
        customTestModule: String?,
    ) {
        val kotlinFiles = module.files.filter { it.fileName.endsWith(".kt") }
        val testFiles = kotlinFiles.map { it.fileName }
        val additionalFiles = JsAdditionalSourceProvider.getAdditionalKotlinFiles(directory).map { it.absolutePath }
        val allSourceFiles = (testFiles + additionalFiles).map(::File)
        val psiFiles = createPsiFiles(allSourceFiles.sortedBy { it.canonicalPath }.map { it.canonicalPath })

        val sourceDirs = (testFiles + additionalFiles).map { File(it).parent }.distinct()
        val config = createConfig(
            sourceDirs,
            module,
            dependencies,
            allDependencies,
            friends,
            multiModule,
            tmpDir,
            incrementalData = null,
            expectActualLinker = expectActualLinker,
            errorIgnorancePolicy
        )
        val outputFile = File(outputFileName)
        val dceOutputFile = File(dceOutputFileName)
        val pirOutputFile = File(pirOutputFileName)

        val incrementalData = IncrementalData()
        translateFiles(
            psiFiles.map(TranslationUnit::SourceFile),
            outputFile,
            dceOutputFile,
            pirOutputFile,
            config,
            outputPrefixFile,
            outputPostfixFile,
            mainCallParameters,
            incrementalData,
            remap,
            testPackage,
            testFunction,
            needsFullIrRuntime,
            isMainModule,
            skipDceDriven,
            esModules,
            granularity,
            propertyLazyInitialization,
            safeExternalBoolean,
            safeExternalBooleanDiagnostic,
            skipMangleVerification,
            abiVersion,
            checkIC,
            recompile = false,
            icCache,
            customTestModule,
        )

        if (checkIC && incrementalCompilationChecksEnabled && module.hasFilesToRecompile) {
            checkIncrementalCompilation(
                sourceDirs,
                module,
                kotlinFiles,
                dependencies,
                allDependencies,
                friends,
                multiModule,
                tmpDir,
                remap,
                outputFile,
                outputPrefixFile,
                outputPostfixFile,
                mainCallParameters,
                incrementalData,
                testPackage,
                testFunction,
                needsFullIrRuntime,
                expectActualLinker,
                icCache
            )
        }
    }

    protected open fun checkIncrementalCompilation(
        sourceDirs: List<String>,
        module: TestModule,
        kotlinFiles: List<TestFile>,
        dependencies: List<String>,
        allDependencies: List<String>,
        friends: List<String>,
        multiModule: Boolean,
        tmpDir: File,
        remap: Boolean,
        outputFile: File,
        outputPrefixFile: File?,
        outputPostfixFile: File?,
        mainCallParameters: MainCallParameters,
        incrementalData: IncrementalData,
        testPackage: String?,
        testFunction: String,
        needsFullIrRuntime: Boolean,
        expectActualLinker: Boolean,
        icCaches: MutableMap<String, TestModuleCache>
    ) {
        val sourceToTranslationUnit = hashMapOf<File, TranslationUnit>()
        for (testFile in kotlinFiles) {
            if (testFile.recompile) {
                val sourceFile = File(testFile.fileName)
                incrementalData.translatedFiles.remove(sourceFile)
                sourceToTranslationUnit[sourceFile] = TranslationUnit.SourceFile(createPsiFile(testFile.fileName))
                incrementalData.packageMetadata.remove(testFile.packageName)
            }
        }
        for ((sourceFile, data) in incrementalData.translatedFiles) {
            sourceToTranslationUnit[sourceFile] = TranslationUnit.BinaryAst(data.binaryAst, data.inlineData)
        }
        val translationUnits = sourceToTranslationUnit.keys
            .sortedBy { it.canonicalPath }
            .map { sourceToTranslationUnit[it]!! }

        val recompiledConfig = createConfig(
            sourceDirs,
            module,
            dependencies,
            allDependencies,
            friends,
            multiModule,
            tmpDir,
            incrementalData,
            expectActualLinker,
            ErrorTolerancePolicy.DEFAULT
        )
        val recompiledOutputFile = File(outputFile.parentFile, outputFile.nameWithoutExtension + "-recompiled.js")

        translateFiles(
            translationUnits,
            recompiledOutputFile,
            recompiledOutputFile,
            recompiledOutputFile,
            recompiledConfig,
            outputPrefixFile,
            outputPostfixFile,
            mainCallParameters,
            incrementalData,
            remap,
            testPackage,
            testFunction,
            needsFullIrRuntime,
            isMainModule = false,
            skipDceDriven = true,
            esModules = false,
            granularity = JsGenerationGranularity.WHOLE_PROGRAM,
            propertyLazyInitialization = true,
            safeExternalBoolean = false,
            safeExternalBooleanDiagnostic = null,
            skipMangleVerification = false,
            abiVersion = KotlinAbiVersion.CURRENT,
            incrementalCompilation = true,
            recompile = true,
            mutableMapOf(),
            customTestModule = null,
        )

        val originalOutput = FileUtil.loadFile(outputFile)
        val recompiledOutput = removeRecompiledSuffix(FileUtil.loadFile(recompiledOutputFile))
        assertEquals("Output file changed after recompilation", originalOutput, recompiledOutput)

        val originalSourceMap = FileUtil.loadFile(File(outputFile.parentFile, outputFile.name + ".map"))
        val recompiledSourceMap =
            removeRecompiledSuffix(FileUtil.loadFile(File(recompiledOutputFile.parentFile, recompiledOutputFile.name + ".map"))
        )
        if (originalSourceMap != recompiledSourceMap) {
            val originalSourceMapParse = SourceMapParser.parse(originalSourceMap)
            val recompiledSourceMapParse = SourceMapParser.parse(recompiledSourceMap)
            if (originalSourceMapParse is SourceMapSuccess && recompiledSourceMapParse is SourceMapSuccess) {
                assertEquals(
                    "Source map file changed after recompilation",
                    originalSourceMapParse.toDebugString(),
                    recompiledSourceMapParse.toDebugString()
                )
            }
            assertEquals("Source map file changed after recompilation", originalSourceMap, recompiledSourceMap)
        }

        if (multiModule) {
            val originalMetadata = FileUtil.loadFile(File(outputFile.parentFile, outputFile.nameWithoutExtension + ".meta.js"))
            val recompiledMetadata = removeRecompiledSuffix(
                FileUtil.loadFile(File(recompiledOutputFile.parentFile, recompiledOutputFile.nameWithoutExtension + ".meta.js"))
            )
            assertEquals(
                "Metadata file changed after recompilation",
                metadataAsString(originalMetadata),
                metadataAsString(recompiledMetadata)
            )
        }
    }

    private fun SourceMapSuccess.toDebugString(): String {
        val out = ByteArrayOutputStream()
        PrintStream(out).use { value.debug(it) }
        return String(out.toByteArray(), Charset.forName("UTF-8"))
    }

    private fun metadataAsString(metadataText: String): String {
        val metadata = mutableListOf<KotlinJavascriptMetadata>().apply {
            KotlinJavascriptMetadataUtils.parseMetadata(metadataText, this)
        }.single()
        val metadataParts = KotlinJavascriptSerializationUtil.readModuleAsProto(metadata.body, metadata.version).body
        return metadataParts.joinToString("-----\n") {
            val binary = it.toByteArray()
            DebugProtoBuf.PackageFragment.parseFrom(binary, JsSerializerProtocol.extensionRegistry).toString()
        }
    }

    private fun removeRecompiledSuffix(text: String): String = text.replace("-recompiled.js", ".js")

    class IncrementalData(
        var header: ByteArray? = null,
        val translatedFiles: MutableMap<File, TranslationResultValue> = hashMapOf(),
        val packageMetadata: MutableMap<String, ByteArray> = hashMapOf()
    )

    protected open fun translateFiles(
        units: List<TranslationUnit>,
        outputFile: File,
        dceOutputFile: File,
        pirOutputFile: File,
        config: JsConfig,
        outputPrefixFile: File?,
        outputPostfixFile: File?,
        mainCallParameters: MainCallParameters,
        incrementalData: IncrementalData,
        remap: Boolean,
        testPackage: String?,
        testFunction: String,
        needsFullIrRuntime: Boolean,
        isMainModule: Boolean,
        skipDceDriven: Boolean,
        esModules: Boolean,
        granularity: JsGenerationGranularity,
        propertyLazyInitialization: Boolean,
        safeExternalBoolean: Boolean,
        safeExternalBooleanDiagnostic: RuntimeDiagnostic?,
        skipMangleVerification: Boolean,
        abiVersion: KotlinAbiVersion,
        incrementalCompilation: Boolean,
        recompile: Boolean,
        icCache: MutableMap<String, TestModuleCache>,
        customTestModule: String?,
    ) {
        val translator = K2JSTranslator(config, false)
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

        if (config.moduleKind != ModuleKind.PLAIN) {
            val content = FileUtil.loadFile(outputFile, true)
            val wrappedContent = wrapWithModuleEmulationMarkers(content, moduleId = config.moduleId, moduleKind = config.moduleKind)
            FileUtil.writeToFile(outputFile, wrappedContent)
        }

        config.configuration[JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER]?.let {
            val incrementalService = it as IncrementalResultsConsumerImpl

            for ((srcFile, data) in incrementalService.packageParts) {
                incrementalData.translatedFiles[srcFile] = data
            }

            incrementalData.packageMetadata += incrementalService.packageMetadata

            incrementalData.header = incrementalService.headerMetadata
        }

        JsAstHandler.processUnitsOfJsProgram(translationResult.program, units, targetBackend) { Assert.fail(it) }
        JsSourceMapHandler.checkSourceMap(outputFile, translationResult.program, remap) { expected, actual ->
            TestCase.assertEquals(expected, actual)
        }
    }

    protected fun wrapWithModuleEmulationMarkers(
        content: String,
        moduleKind: ModuleKind,
        moduleId: String
    ): String {
        val escapedModuleId = StringUtil.escapeStringCharacters(moduleId)

        return when (moduleKind) {

            ModuleKind.COMMON_JS -> "$KOTLIN_TEST_INTERNAL.beginModule();\n" +
                    "$content\n" +
                    "$KOTLIN_TEST_INTERNAL.endModule(\"$escapedModuleId\");"

            ModuleKind.AMD, ModuleKind.UMD ->
                "if (typeof $KOTLIN_TEST_INTERNAL !== \"undefined\") { " +
                        "$KOTLIN_TEST_INTERNAL.setModuleId(\"$escapedModuleId\"); }\n" +
                        "$content\n"

            ModuleKind.PLAIN -> content

            ModuleKind.ES -> error("Module emulation markers are not supported for ES modules")
        }
    }

    private fun createPsiFile(fileName: String): KtFile {
        val psiManager = PsiManager.getInstance(project)
        val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

        val file = fileSystem.findFileByPath(fileName) ?: error("File not found: ${fileName}")

        return psiManager.findFile(file) as KtFile
    }

    private fun createPsiFiles(fileNames: List<String>): List<KtFile> = fileNames.map(this::createPsiFile)

    protected fun createConfig(
        sourceDirs: List<String>,
        module: TestModule,
        dependencies: List<String>,
        allDependencies: List<String>,
        friends: List<String>,
        multiModule: Boolean,
        tmpDir: File,
        incrementalData: IncrementalData?,
        expectActualLinker: Boolean,
        errorIgnorancePolicy: ErrorTolerancePolicy,
    ): JsConfig {
        val configuration = environment.configuration.copy()

        configuration.put(CommonConfigurationKeys.DISABLE_INLINE, module.inliningDisabled)
        module.languageVersionSettings?.let { languageVersionSettings ->
            configuration.languageVersionSettings = languageVersionSettings
        }

        val libraries = when (targetBackend) {
            TargetBackend.JS_IR_ES6 -> dependencies
            TargetBackend.JS_IR -> dependencies
            TargetBackend.JS -> JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST + dependencies
            else -> error("Unsupported target backend: $targetBackend")
        }

        configuration.put(JSConfigurationKeys.LIBRARIES, libraries)
        configuration.put(JSConfigurationKeys.TRANSITIVE_LIBRARIES, allDependencies)
        configuration.put(JSConfigurationKeys.FRIEND_PATHS, friends)

        configuration.put(CommonConfigurationKeys.MODULE_NAME, module.name.removeSuffix(OLD_MODULE_SUFFIX))
        configuration.put(JSConfigurationKeys.MODULE_KIND, module.moduleKind)
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.v5)
        configuration.put(JSConfigurationKeys.ERROR_TOLERANCE_POLICY, errorIgnorancePolicy)

        if (errorIgnorancePolicy.allowErrors) {
            configuration.put(JSConfigurationKeys.DEVELOPER_MODE, true)
        }

        val hasFilesToRecompile = module.hasFilesToRecompile
        configuration.put(JSConfigurationKeys.META_INFO, multiModule)
        if (hasFilesToRecompile) {
            val header = incrementalData?.header
            if (header != null) {
                configuration.put(
                    JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER,
                    IncrementalDataProviderImpl(
                        header,
                        incrementalData.translatedFiles,
                        JsMetadataVersion.INSTANCE.toArray(),
                        incrementalData.packageMetadata,
                        emptyMap()
                    )
                )
            }

            configuration.put(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER, IncrementalResultsConsumerImpl())
        }
        configuration.put(JSConfigurationKeys.SOURCE_MAP, true)
        configuration.put(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, sourceDirs)
        configuration.put(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, module.sourceMapSourceEmbedding)

        configuration.put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, typedArraysEnabled)

        configuration.put(JSConfigurationKeys.GENERATE_REGION_COMMENTS, true)

        configuration.put(
            JSConfigurationKeys.FILE_PATHS_PREFIX_MAP,
            mapOf(
                tmpDir.absolutePath to "<TMP>",
                File(".").absolutePath.removeSuffix(".") to ""
            )
        )

        configuration.put(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER, expectActualLinker)

        return JsConfig(project, configuration, CompilerEnvironment, METADATA_CACHE, (JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST).toSet())
    }

    private inner class TestFileFactoryImpl() : TestFiles.TestFileFactory<TestModule, TestFile>, Closeable {
        var testPackage: String? = null
        val tmpDir = KtTestUtil.tmpDir("js-tests")
        val defaultModule = TestModule(TEST_MODULE, emptyList(), emptyList(), KotlinAbiVersion.CURRENT)
        var languageVersionSettings: LanguageVersionSettings? = null

        override fun createFile(module: TestModule?, fileName: String, text: String, directives: Directives): TestFile? {
            val currentModule = module ?: defaultModule

            val ktFile = KtPsiFactory(project).createFile(text)
            val boxFunction = ktFile.declarations.find { it is KtNamedFunction && it.name == TEST_FUNCTION }
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
            KtTestUtil.mkdirs(temporaryFile.parentFile)
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

            parseLanguageVersionSettings(directives)?.trySet()

            // Relies on the order of module creation
            // TODO is that ok?
            currentModule.languageVersionSettings = languageVersionSettings

            SOURCE_MAP_SOURCE_EMBEDDING.find(text)?.let { match ->
                currentModule.sourceMapSourceEmbedding = SourceMapSourceEmbedding.valueOf(match.groupValues[1])
            }

            return TestFile(
                temporaryFile.absolutePath,
                currentModule,
                recompile = RECOMPILE_PATTERN.matcher(text).find(),
                packageName = ktFile.packageFqName.asString(),
                testEntryEsModule = ENTRY_ES_MODULE.matcher(text).find(),
            )
        }

        override fun createModule(name: String, dependencies: List<String>, friends: List<String>, abiVersions: List<Int>): TestModule {
            val abiVersion = if (abiVersions.isEmpty()) KotlinAbiVersion.CURRENT else {
                assert(abiVersions.size == 3)
                KotlinAbiVersion(abiVersions[0], abiVersions[1], abiVersions[2])
            }
            return TestModule(name, dependencies, friends, abiVersion)
        }

        override fun close() {
            FileUtil.delete(tmpDir)
        }
    }

    protected class TestFile(val fileName: String, val module: TestModule, val recompile: Boolean, val packageName: String, val testEntryEsModule: Boolean) {
        init {
            module.files += this
        }
    }

    protected class TestModule(
        name: String,
        dependencies: List<String>,
        friends: List<String>,
        val abiVersion: KotlinAbiVersion
    ) : KotlinBaseTest.TestModule(name, dependencies, friends) {
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
        val METADATA_CACHE = (JsConfig.JS_STDLIB + JsConfig.JS_KOTLIN_TEST).flatMap { path ->
            KotlinJavascriptMetadataUtils.loadMetadata(path).map { metadata ->
                val parts = KotlinJavascriptSerializationUtil.readModuleAsProto(metadata.body, metadata.version)
                JsModuleDescriptor(metadata.moduleName, parts.kind, parts.importedModules, parts)
            }
        }

        const val TEST_DATA_DIR_PATH = "js/js.translator/testData/"
        const val DIST_DIR_JS_PATH = "dist/js/"

        private const val MODULE_EMULATION_FILE = TEST_DATA_DIR_PATH + "/moduleEmulation.js"

        private val MODULE_KIND_PATTERN = Pattern.compile("^// *MODULE_KIND: *(.+)$", Pattern.MULTILINE)
        private val NO_MODULE_SYSTEM_PATTERN = Pattern.compile("^// *NO_JS_MODULE_SYSTEM", Pattern.MULTILINE)

        // Infer main module using dependency graph
        private val INFER_MAIN_MODULE = Pattern.compile("^// *INFER_MAIN_MODULE", Pattern.MULTILINE)

        // Run top level box function
        private val RUN_PLAIN_BOX_FUNCTION = Pattern.compile("^// *RUN_PLAIN_BOX_FUNCTION", Pattern.MULTILINE)

        private val ENTRY_ES_MODULE = Pattern.compile("^// *ENTRY_ES_MODULE", Pattern.MULTILINE)

        private val NO_INLINE_PATTERN = Pattern.compile("^// *NO_INLINE *$", Pattern.MULTILINE)
        private val SKIP_NODE_JS = Pattern.compile("^// *SKIP_NODE_JS *$", Pattern.MULTILINE)
        private val SKIP_MINIFICATION = Pattern.compile("^// *SKIP_MINIFICATION *$", Pattern.MULTILINE)
        private val SKIP_SOURCEMAP_REMAPPING = Pattern.compile("^// *SKIP_SOURCEMAP_REMAPPING *$", Pattern.MULTILINE)
        private val EXPECTED_REACHABLE_NODES_DIRECTIVE = "EXPECTED_REACHABLE_NODES"
        private val EXPECTED_REACHABLE_NODES = Pattern.compile("^// *$EXPECTED_REACHABLE_NODES_DIRECTIVE: *([0-9]+) *$", Pattern.MULTILINE)
        private val RECOMPILE_PATTERN = Pattern.compile("^// *RECOMPILE *$", Pattern.MULTILINE)
        private val SOURCE_MAP_SOURCE_EMBEDDING = Regex("^// *SOURCE_MAP_EMBED_SOURCES: ([A-Z]+)*\$", RegexOption.MULTILINE)
        private val CALL_MAIN_PATTERN = Pattern.compile("^// *CALL_MAIN *$", Pattern.MULTILINE)
        private val KJS_WITH_FULL_RUNTIME = Pattern.compile("^// *KJS_WITH_FULL_RUNTIME *\$", Pattern.MULTILINE)
        private val WITH_RUNTIME = Pattern.compile("^// *WITH_RUNTIME *\$", Pattern.MULTILINE)
        private val WITH_STDLIB = Pattern.compile("^// *WITH_STDLIB *\$", Pattern.MULTILINE)
        private val EXPECT_ACTUAL_LINKER = Pattern.compile("^// EXPECT_ACTUAL_LINKER *$", Pattern.MULTILINE)
        private val SKIP_DCE_DRIVEN = Pattern.compile("^// *SKIP_DCE_DRIVEN *$", Pattern.MULTILINE)
        private val ES_MODULES = Pattern.compile("^// *ES_MODULES *$", Pattern.MULTILINE)

        private val SPLIT_PER_MODULE = Pattern.compile("^// *SPLIT_PER_MODULE *$", Pattern.MULTILINE)
        private val SPLIT_PER_FILE = Pattern.compile("^// *SPLIT_PER_FILE *$", Pattern.MULTILINE)
        private val SKIP_MANGLE_VERIFICATION = Pattern.compile("^// *SKIP_MANGLE_VERIFICATION *$", Pattern.MULTILINE)

        private val ERROR_POLICY_PATTERN = Pattern.compile("^// *ERROR_POLICY: *(.+)$", Pattern.MULTILINE)

        private val SAFE_EXTERNAL_BOOLEAN = Pattern.compile("^// *SAFE_EXTERNAL_BOOLEAN *$", Pattern.MULTILINE)
        private val SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC = Pattern.compile("^// *SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC: *(.+)$", Pattern.MULTILINE)

        private val SKIP_IR_INCREMENTAL_CHECKS = Pattern.compile("^// *SKIP_IR_INCREMENTAL_CHECKS *$", Pattern.MULTILINE)

        @JvmStatic
        protected val runTestInNashorn = getBoolean("kotlin.js.useNashorn")

        const val TEST_MODULE = "main"
        const val DEFAULT_MODULE = "main"
        private const val TEST_FUNCTION = "box"
        private const val OLD_MODULE_SUFFIX = "_old"

        const val KOTLIN_TEST_INTERNAL = "\$kotlin_test_internal\$"
    }
}

fun KotlinTestWithEnvironment.createPsiFile(fileName: String): KtFile {
    val psiManager = PsiManager.getInstance(project)
    val fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)

    val file = fileSystem.findFileByPath(fileName) ?: error("File not found: $fileName")

    return psiManager.findFile(file) as KtFile
}

fun KotlinTestWithEnvironment.createPsiFiles(fileNames: List<String>): List<KtFile> = fileNames.map(this::createPsiFile)

class KotlinJsTestLogger {
    val verbose = getBoolean("kotlin.js.test.verbose")

    fun logFile(description: String, file: File) {
        if (verbose) {
            println("TEST_LOG: $description file://${file.absolutePath}")
        }
    }
}

fun RuntimeDiagnostic.Companion.resolve(
    value: String?,
): RuntimeDiagnostic? = when (value?.lowercase()) {
    K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_LOG -> RuntimeDiagnostic.LOG
    K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_EXCEPTION -> RuntimeDiagnostic.EXCEPTION
    null -> null
    else -> {
        null
    }
}

val v8tool by lazy { ExternalTool(System.getProperty("javascript.engine.path.V8")) }