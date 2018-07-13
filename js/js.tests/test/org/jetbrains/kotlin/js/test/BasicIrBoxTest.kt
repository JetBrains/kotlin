/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
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
    "core/builtins/native/kotlin/Number.kt",
    "core/builtins/native/kotlin/Comparable.kt",
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
        testFunction: String
    ) {
        val filesToCompile = units
            .map { (it as TranslationUnit.SourceFile).file }
            // TODO: split input files to some parts (global common, local common, test)
            .filterNot { it.virtualFilePath.contains(BasicBoxTest.COMMON_FILES_DIR_PATH) }

        if (runtimeResult == null) {
            val myConfiguration = config.configuration.copy()

            // TODO: is it right in general? Maybe sometimes we need to compile with newer versions or with additional language features.
            myConfiguration.languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)

            runtimeResult = compile(config.project, runtimeSources.map(::createPsiFile), myConfiguration)
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
        // TODO: cache runtime.js and don't cache kotlin.js for IR BE tests
        super.runGeneratedCode(listOf(runtimeFile.path) + jsFiles, null, null, testFunction, expectedResult, false)
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
