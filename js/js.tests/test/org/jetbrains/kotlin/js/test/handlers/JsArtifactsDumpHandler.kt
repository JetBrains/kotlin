/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.test.WrappedException
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.AfterAnalysisChecker
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

/**
 * Copy JS artifacts from the temporary directory to the `js/js.tests/build/out` directory.
 */
class JsArtifactsDumpHandler(testServices: TestServices) : AfterAnalysisChecker(testServices) {

    override fun check(failedAssertions: List<WrappedException>) {
        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
        val allDirectives = testServices.moduleStructure.allDirectives

        val stopFile = File(allDirectives[JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR].first())
        val pathToRootOutputDir = allDirectives[JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR].first()
        val testGroupOutputDirPrefix = allDirectives[JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX].first()

        val testGroupOutputDirForCompilation = File(pathToRootOutputDir + "out/" + testGroupOutputDirPrefix)
        val testGroupOutputDirForMinification = File(pathToRootOutputDir + "out-min/" + testGroupOutputDirPrefix)
        val testGroupOutputDirForPerModuleCompilation = File(pathToRootOutputDir + "out-per-module/" + testGroupOutputDirPrefix)
        val testGroupOutputDirForPerModuleMinification = File(pathToRootOutputDir + "out-per-module-min/" + testGroupOutputDirPrefix)
        val testGroupOutputDirForPerFileCompilation = File(pathToRootOutputDir + "out-per-file/" + testGroupOutputDirPrefix)
        val testGroupOutputDirForPerFileMinification = File(pathToRootOutputDir + "out-per-file-min/" + testGroupOutputDirPrefix)

        val outputDir = getOutputDir(originalFile, testGroupOutputDirForCompilation, stopFile)
        val dceOutputDir = getOutputDir(originalFile, testGroupOutputDirForMinification, stopFile)
        val perModuleOutputDir = getOutputDir(originalFile, testGroupOutputDirForPerModuleCompilation, stopFile)
        val perModuleDceOutputDir = getOutputDir(originalFile, testGroupOutputDirForPerModuleMinification, stopFile)
        val perFileOutputDir = getOutputDir(originalFile, testGroupOutputDirForPerFileCompilation, stopFile)
        val perFileDceOutputDir = getOutputDir(originalFile, testGroupOutputDirForPerFileMinification, stopFile)
        val minOutputDir = File(dceOutputDir, originalFile.nameWithoutExtension)

        copy(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices), outputDir)
        copy(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, TranslationMode.FULL_PROD_MINIMIZED_NAMES), dceOutputDir)
        copy(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, TranslationMode.PER_MODULE_DEV), perModuleOutputDir)
        copy(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, TranslationMode.PER_MODULE_PROD_MINIMIZED_NAMES), perModuleDceOutputDir)
        copy(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, TranslationMode.PER_FILE_DEV), perFileOutputDir)
        copy(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices, TranslationMode.PER_FILE_PROD_MINIMIZED_NAMES), perFileDceOutputDir)
        copy(JsEnvironmentConfigurator.getMinificationJsArtifactsOutputDir(testServices), minOutputDir)
    }

    private fun getOutputDir(file: File, testGroupOutputDir: File, stopFile: File): File {
        return generateSequence(file.parentFile) { it.parentFile }
            .takeWhile { it != stopFile }
            .map { it.name }
            .toList().asReversed()
            .fold(testGroupOutputDir, ::File)
    }

    private fun copy(from: File, into: File) {
        if (from.listFiles()?.size == 0) return
        from.copyRecursively(into, overwrite = true)
    }
}
