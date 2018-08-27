/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.backend.js.Result
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

private val runtimeSources = listOfKtFilesFrom(
    "libraries/stdlib/js/src/kotlin/core.kt",
    "libraries/stdlib/js/src/kotlin/js.core.kt",
    "libraries/stdlib/js/src/kotlin/jsTypeOf.kt",
    "libraries/stdlib/js/src/kotlin/dynamic.kt",
    "libraries/stdlib/js/src/kotlin/annotations.kt",
    "libraries/stdlib/js/src/kotlin/reflect",
    "libraries/stdlib/js/src/kotlin/annotationsJVM.kt",

    "libraries/stdlib/js/runtime/primitiveCompanionObjects.kt",

    "libraries/stdlib/src/kotlin/internal",
    "libraries/stdlib/src/kotlin/contracts",
    "libraries/stdlib/src/kotlin/annotations/Experimental.kt",
    "libraries/stdlib/src/kotlin/util/Standard.kt",
    "core/builtins/native/kotlin/Annotation.kt",
    "core/builtins/native/kotlin/Number.kt",
    "core/builtins/native/kotlin/Comparable.kt",
    "core/builtins/src/kotlin/Annotations.kt",
    "core/builtins/src/kotlin/internal/InternalAnnotations.kt",
    "core/builtins/src/kotlin/internal/progressionUtil.kt",
    "core/builtins/src/kotlin/Iterators.kt",
    "core/builtins/src/kotlin/ProgressionIterators.kt",
    "core/builtins/src/kotlin/Progressions.kt",
    "core/builtins/src/kotlin/Range.kt",
    "core/builtins/src/kotlin/Ranges.kt",
    "core/builtins/src/kotlin/Unit.kt",
    "core/builtins/src/kotlin/reflect",
    "core/builtins/src/kotlin/Function.kt",

    "core/builtins/native/kotlin/Collections.kt",
    "core/builtins/native/kotlin/Iterator.kt",

    "libraries/stdlib/common/src/kotlin/JsAnnotationsH.kt",

    "libraries/stdlib/js/irRuntime",
    BasicBoxTest.COMMON_FILES_DIR_PATH
)

private var runtimeResult: Result? = null
private val runtimeFile = File("js/js.translator/testData/out/irBox/testRuntime.js")

abstract class BasicIrBoxTest(
    pathToTestDir: String,
    testGroupOutputDirPrefix: String,
    pathToRootOutputDir: String = BasicBoxTest.TEST_DATA_DIR_PATH,
    generateSourceMap: Boolean = false,
    generateNodeJsRunner: Boolean = false
) : BasicBoxTest(
    pathToTestDir,
    testGroupOutputDirPrefix,
    pathToRootOutputDir = pathToRootOutputDir,
    typedArraysEnabled = true,
    generateSourceMap = generateSourceMap,
    generateNodeJsRunner = generateNodeJsRunner,
    targetBackend = TargetBackend.JS_IR
) {

    override var skipMinification = true

    override fun translateFiles(
        units: List<TranslationUnit>,
        outputFile: File,
        config: JsConfig,
        outputPrefixFile: File?,
        outputPostfixFile: File?,
        mainCallParameters: MainCallParameters,
        incrementalData: IncrementalData,
        remap: Boolean,
        testPackage: String?,
        testFunction: String,
        doNotCache: Boolean
    ) {
        val filesToCompile = units
            .map { (it as TranslationUnit.SourceFile).file }
            // TODO: split input files to some parts (global common, local common, test)
            .filterNot { it.virtualFilePath.contains(BasicBoxTest.COMMON_FILES_DIR_PATH) }

        val runtimeConfiguration = config.configuration.copy()

        // TODO: is it right in general? Maybe sometimes we need to compile with newer versions or with additional language features.
        runtimeConfiguration.languageVersionSettings = LanguageVersionSettingsImpl(
            LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE,
            specificFeatures = mapOf(
                LanguageFeature.AllowContractsForCustomFunctions to LanguageFeature.State.ENABLED,
                LanguageFeature.MultiPlatformProjects to LanguageFeature.State.ENABLED
            ),
            analysisFlags = mapOf(
                AnalysisFlag.useExperimental to listOf("kotlin.contracts.ExperimentalContracts", "kotlin.Experimental")
            )
        )


        if (runtimeResult == null) {
            runtimeResult = compile(config.project, runtimeSources.map(::createPsiFile), runtimeConfiguration)
            runtimeFile.write(runtimeResult!!.generatedCode)
        }

        val result = if (doNotCache) {
            val runtimeFiles = runtimeSources.map(::createPsiFile)
            val allFiles = runtimeFiles + filesToCompile
            compile(
                config.project,
                allFiles,
                runtimeConfiguration,
                FqName((testPackage?.let { "$it." } ?: "") + testFunction))
        } else {
            compile(
                config.project,
                filesToCompile,
                config.configuration,
                FqName((testPackage?.let { "$it." } ?: "") + testFunction),
                listOf(runtimeResult!!.moduleDescriptor))
        }

        outputFile.write(result.generatedCode)
    }

    override fun runGeneratedCode(
        jsFiles: List<String>,
        testModuleName: String?,
        testPackage: String?,
        testFunction: String,
        expectedResult: String,
        withModuleSystem: Boolean
    ) {
        // TODO: should we do anything special for module systems?
        // TODO: return list of js from translateFiles and provide then to this function with other js files
        NashornIrJsTestChecker.check(jsFiles, null, null, testFunction, expectedResult, false)
    }
}

private fun listOfKtFilesFrom(vararg paths: String): List<String> {
    val currentDir = File(".")
    return paths.flatMap { path ->
        File(path)
            .walkTopDown()
            .filter { it.extension == "kt" }
            .map { it.relativeToOrSelf(currentDir).path }
            .asIterable()
    }
}

private fun File.write(text: String) {
    parentFile.mkdirs()
    writeText(text)
}
