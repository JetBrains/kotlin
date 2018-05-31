/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test

import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.TranslationUnit
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

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
        val runtime = listOf(
            "libraries/stdlib/js/src/kotlin/core.kt",
            "libraries/stdlib/js/irRuntime/annotations.kt",
            "libraries/stdlib/js/irRuntime/internalAnnotations.kt",
            "libraries/stdlib/js/irRuntime/typeCheckUtils.kt"
        ).map { createPsiFile(it) }

        val filesToIgnore = listOf(
            // TODO: temporary ignore some files from _commonFiles directory since they can't be compiled correctly by JS IR BE yet.
            // Also, some declarations depends on stdlib but we don't have any library support in JS IR BE yet
            // and probably it will be better to avoid using stdlib in testData as much as possible.
            "js/js.translator/testData/_commonFiles/arrayAsserts.kt"
        )

        val filesToCompile = units
            .map { (it as TranslationUnit.SourceFile).file }
            .filter { file -> filesToIgnore.none { file.virtualFilePath.endsWith(it) } }

        val code = compile(
            config.project,
            runtime + filesToCompile,
            config.configuration,
            FqName((testPackage?.let { "$it." } ?: "") + testFunction))

        outputFile.parentFile.mkdirs()
        outputFile.writeText(code)
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
        super.runGeneratedCode(jsFiles, null, null, testFunction, expectedResult, false)
    }
}
