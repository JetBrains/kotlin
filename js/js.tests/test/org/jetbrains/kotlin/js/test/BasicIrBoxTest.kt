/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.backend.js.Result
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

// Required to compile native builtins with the rest of runtime
private const val builtInsHeader = """@file:Suppress(
    "NON_ABSTRACT_FUNCTION_WITH_NO_BODY",
    "MUST_BE_INITIALIZED_OR_BE_ABSTRACT",
    "EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE",
    "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED",
    "WRONG_MODIFIER_TARGET"
)
"""

// Native builtins that were not implemented in JS IR runtime
private val unimplementedNativeBuiltInsDir = KotlinTestUtils.tmpDir("unimplementedBuiltins").also { tmpDir ->
    val allBuiltins = listOfKtFilesFrom("core/builtins/native/kotlin").map { File(it).name }
    val implementedBuiltIns = listOfKtFilesFrom("libraries/stdlib/js/irRuntime/builtins/").map { File(it).name }
    val unimplementedBuiltIns = allBuiltins - implementedBuiltIns
    for (filename in unimplementedBuiltIns) {
        val originalFile = File("core/builtins/native/kotlin", filename)
        val newFile = File(tmpDir, filename)
        val sourceCode = builtInsHeader + originalFile.readText()
        newFile.writeText(sourceCode)
    }
}

private val runtimeSources = listOfKtFilesFrom(
    "core/builtins/src/kotlin",
    "libraries/stdlib/common/src",
    "libraries/stdlib/src/kotlin/",
    "libraries/stdlib/js/src/kotlin",
    "libraries/stdlib/js/irRuntime",
    "libraries/stdlib/js/runtime",
    "libraries/stdlib/unsigned",
    unimplementedNativeBuiltInsDir.path,
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

    // IR BE has its own generated sources
    "libraries/stdlib/js/src/kotlin/collectionsExternal.kt",

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

//        config.configuration.put(CommonConfigurationKeys.EXCLUDED_ELEMENTS_FROM_DUMPING, setOf("<JS_IR_RUNTIME>"))
        config.configuration.put(
            CommonConfigurationKeys.PHASES_TO_VALIDATE_AFTER,
            setOf(
                "RemoveInlineFunctionsWithReifiedTypeParametersLowering",
                "InnerClassConstructorCallsLowering",
                "InlineClassLowering", "ConstLowering"
            )
        )

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
            runtimeConfiguration.put(CommonConfigurationKeys.MODULE_NAME, "JS_IR_RUNTIME")
            runtimeResult = compile(config.project, runtimeSources.map(::createPsiFile), runtimeConfiguration)
            runtimeFile.write(runtimeResult!!.generatedCode)
        }

        val runtime = runtimeResult!!

        val dependencyNames = config.configuration[JSConfigurationKeys.LIBRARIES]!!.map { File(it).name }
        val dependencies = listOf(runtime.moduleDescriptor) + dependencyNames.mapNotNull { compilationCache[it]?.moduleDescriptor }
        val irDependencies = listOf(runtime.moduleFragment) + compilationCache.values.map { it.moduleFragment }

//        config.configuration.put(CommonConfigurationKeys.PHASES_TO_DUMP_STATE, setOf("UnitMaterializationLowering"))
//        config.configuration.put(CommonConfigurationKeys.PHASES_TO_DUMP_STATE_BEFORE, setOf("ReturnableBlockLowering"))
//        config.configuration.put(CommonConfigurationKeys.PHASES_TO_DUMP_STATE_AFTER, setOf("MultipleCatchesLowering"))
//        config.configuration.put(CommonConfigurationKeys.PHASES_TO_VALIDATE, setOf("ALL"))

        val result = compile(
            config.project,
            filesToCompile,
            config.configuration,
            FqName((testPackage?.let { "$it." } ?: "") + testFunction),
            dependencies,
            irDependencies,
            runtime.moduleDescriptor as ModuleDescriptorImpl)

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
