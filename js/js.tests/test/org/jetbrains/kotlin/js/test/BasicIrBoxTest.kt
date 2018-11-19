/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.backend.js.Result
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

private val runtimeSources = listOfKtFilesFrom(
    "core/builtins/src/kotlin",
    "libraries/stdlib/common/src",
    "libraries/stdlib/src/kotlin/",
    "libraries/stdlib/js/src/kotlin",
    "libraries/stdlib/js/src/generated",
    "libraries/stdlib/js/irRuntime",
    "libraries/stdlib/js/runtime",
    "libraries/stdlib/unsigned",

    "core/builtins/native/kotlin/Annotation.kt",
    "core/builtins/native/kotlin/Number.kt",
    "core/builtins/native/kotlin/Comparable.kt",
    "core/builtins/native/kotlin/Collections.kt",
    "core/builtins/native/kotlin/Iterator.kt",
    "core/builtins/native/kotlin/CharSequence.kt",

    "core/builtins/src/kotlin/Unit.kt",

    BasicBoxTest.COMMON_FILES_DIR_PATH
) - listOfKtFilesFrom(
    "libraries/stdlib/common/src/kotlin/JvmAnnotationsH.kt",
    "libraries/stdlib/src/kotlin/annotations/Multiplatform.kt",

    // TODO: Support Int.pow
    "libraries/stdlib/js/src/kotlin/random/PlatformRandom.kt",

    // Fails with: EXPERIMENTAL_IS_NOT_ENABLED
    "libraries/stdlib/common/src/kotlin/annotations/Annotations.kt",

    // Conflicts with libraries/stdlib/js/src/kotlin/annotations.kt
    "libraries/stdlib/js/runtime/hacks.kt",

    // TODO: Reuse in IR BE
    "libraries/stdlib/js/runtime/Enum.kt",

    // JS-specific optimized version of emptyArray() already defined
    "core/builtins/src/kotlin/ArrayIntrinsics.kt",

    // Unnecessary for now
    "libraries/stdlib/js/src/kotlin/dom",
    "libraries/stdlib/js/src/kotlin/browser",

    // TODO: fix compilation issues in arrayPlusCollection
    // Replaced with irRuntime/kotlinHacks.kt
    "libraries/stdlib/js/src/kotlin/kotlin.kt",

    "libraries/stdlib/js/src/kotlin/currentBeMisc.kt",

    // Full version is defined in stdlib
    // This file is useful for smaller subset of runtime sources
    "libraries/stdlib/js/irRuntime/rangeExtensions.kt",

    // Mostly array-specific stuff
    "libraries/stdlib/js/src/kotlin/builtins.kt",

    // coroutines
    // TODO: merge coroutines_13 with JS BE coroutines
    "libraries/stdlib/js/src/kotlin/coroutines/intrinsics/IntrinsicsJs.kt",
    "libraries/stdlib/js/src/kotlin/coroutines/CoroutineImpl.kt",

    // Inlining of js fun doesn't update the variables inside
    "libraries/stdlib/js/src/kotlin/jsTypeOf.kt",
    "libraries/stdlib/js/src/kotlin/collections/utils.kt"
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

    override val skipMinification = true

    // TODO Design incremental compilation for IR and add test support
    override val incrementalCompilationChecksEnabled = false

    private val compilationCache = mutableMapOf<String, Result>()

    override fun doTest(filePath: String, expectedResult: String, mainCallParameters: MainCallParameters, coroutinesPackage: String) {
        compilationCache.clear()
        super.doTest(filePath, expectedResult, mainCallParameters, coroutinesPackage)
    }

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
        testFunction: String
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
                AnalysisFlags.useExperimental to listOf("kotlin.contracts.ExperimentalContracts", "kotlin.Experimental"),
                AnalysisFlags.allowResultReturnType to true
            )
        )

        if (runtimeResult == null) {
            runtimeResult = compile(config.project, runtimeSources.map(::createPsiFile), runtimeConfiguration)
            runtimeFile.write(runtimeResult!!.generatedCode)
        }

        val dependencyNames = config.configuration[JSConfigurationKeys.LIBRARIES]!!.map { File(it).name }
        val dependencies = listOf(runtimeResult!!.moduleDescriptor) + dependencyNames.mapNotNull { compilationCache[it]?.moduleDescriptor }
        val irDependencies = listOf(runtimeResult!!.moduleFragment) + compilationCache.values.map { it.moduleFragment }

        val result = compile(
            config.project,
            filesToCompile,
            config.configuration,
            FqName((testPackage?.let { "$it." } ?: "") + testFunction),
            dependencies,
            irDependencies)

        compilationCache[outputFile.name.replace(".js", ".meta.js")] = result

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
    }.distinct()
}

private fun File.write(text: String) {
    parentFile.mkdirs()
    writeText(text)
}
