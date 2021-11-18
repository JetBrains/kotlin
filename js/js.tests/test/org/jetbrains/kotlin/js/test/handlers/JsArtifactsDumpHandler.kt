/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers

import org.jetbrains.kotlin.test.backend.handlers.JsBinaryArtifactHandler
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

class JsArtifactsDumpHandler(testServices: TestServices) : JsBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Js) {}

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val originalFile = testServices.moduleStructure.originalTestDataFiles.first()
        val allDirectives = testServices.moduleStructure.allDirectives

        val stopFile = File(allDirectives[JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR].first())
        val pathToRootOutputDir = allDirectives[JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR].first()
        val testGroupOutputDirPrefix = allDirectives[JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX].first()

        val testGroupOutputDirForCompilation = File(pathToRootOutputDir + "out/" + testGroupOutputDirPrefix)
        val testGroupOutputDirForMinification = File(pathToRootOutputDir + "out-min/" + testGroupOutputDirPrefix)

        val outputDir = getOutputDir(originalFile, testGroupOutputDirForCompilation, stopFile)
        val dceOutputDir = getOutputDir(originalFile, testGroupOutputDirForMinification, stopFile)
        val minOutputDir = File(dceOutputDir, originalFile.nameWithoutExtension)

        copy(JsEnvironmentConfigurator.getJsArtifactsOutputDir(testServices), outputDir)
        copy(JsEnvironmentConfigurator.getDceJsArtifactsOutputDir(testServices), dceOutputDir)
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