/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.js.JavaScript
import org.jetbrains.kotlin.test.backend.handlers.WasmBinaryArtifactHandler
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

abstract class AbstractWasmArtifactsCollector(testServices: TestServices) :
    WasmBinaryArtifactHandler(testServices), WasmArtifactsCollector {
    val modulesToArtifact = mutableMapOf<TestModule, BinaryArtifacts.Wasm>()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        modulesToArtifact[module] = info
    }
}

fun TestServices.getWasmTestOutputDirectory(): File {
    val originalFile = moduleStructure.originalTestDataFiles.first()
    val allDirectives = moduleStructure.allDirectives

    val pathToRootOutputDir = allDirectives[WasmEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR].first()
    val testGroupDirPrefix = allDirectives[WasmEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX].first()
    val pathToTestDir = allDirectives[WasmEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR].first()

    val testGroupOutputDir = File(File(pathToRootOutputDir, "out"), testGroupDirPrefix)
    val stopFile = ForTestCompileRuntime.transformTestDataPath(pathToTestDir).absoluteFile
    val parentAbsoluteFile = originalFile.parentFile.absoluteFile
    val fullPathSequence = generateSequence(parentAbsoluteFile) { it.parentFile }.toList()
    val suffixPathSequence = fullPathSequence.takeWhile { it != stopFile }
    require(suffixPathSequence.size < fullPathSequence.size) {
        "Folder $stopFile (which is set by PATH_TO_TEST_DIR directive) must contain $parentAbsoluteFile"
    }
    return suffixPathSequence
        .map { it.name }
        .toList().asReversed()
        .fold(testGroupOutputDir, ::File)
        .let { File(it, originalFile.nameWithoutExtension) }
}
