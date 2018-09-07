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
    // TODO: share more coroutine code between JS BEs
    // TODO: don't generate code for external declarations, until that:
    //  it's important now than *H.kt files at the start since we generate code for them
    //  and actual versions just will override them later.
    "libraries/stdlib/coroutines-experimental/src/kotlin/coroutines/experimental/CoroutinesExperimentalH.kt",
    "libraries/stdlib/coroutines-experimental/src/kotlin/coroutines/experimental/CoroutinesIntrinsicsExperimentalH.kt",
    "libraries/stdlib/coroutines-experimental/src/kotlin/coroutines/experimental/SequenceBuilder.kt",
    "libraries/stdlib/coroutines-experimental/src/kotlin/coroutines/experimental/Coroutines.kt",

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

    BasicBoxTest.COMMON_FILES_DIR_PATH
) - listOfKtFilesFrom(
    "libraries/stdlib/common/src/kotlin/JvmAnnotationsH.kt",
    "libraries/stdlib/src/kotlin/annotations/Multiplatform.kt",

    // TODO: Support Int.pow
    "libraries/stdlib/js/src/kotlin/random/PlatformRandom.kt",

    // TODO: Unify exceptions
    "libraries/stdlib/common/src/kotlin/ExceptionsH.kt",

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

    // TODO: Unify exceptions
    "libraries/stdlib/js/src/kotlin/exceptions.kt",

    // TODO: fix compilation issues in arrayPlusCollection
    // Replaced with irRuntime/kotlinHacks.kt
    "libraries/stdlib/js/src/kotlin/kotlin.kt",

    // Full version is defined in stdlib
    // This file is useful for smaller subset of runtime sources
    "libraries/stdlib/js/irRuntime/rangeExtensions.kt"
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
                AnalysisFlag.useExperimental to listOf("kotlin.contracts.ExperimentalContracts", "kotlin.Experimental")
            )
        )


        if (runtimeResult == null) {
            runtimeResult = compile(config.project, runtimeSources.map(::createPsiFile), runtimeConfiguration)
            runtimeFile.write(runtimeResult!!.generatedCode)
        }

        val result = compile(
            config.project,
            filesToCompile,
            config.configuration,
            FqName((testPackage?.let { "$it." } ?: "") + testFunction),
            listOf(runtimeResult!!.moduleDescriptor))

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
