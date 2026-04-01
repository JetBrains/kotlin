/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.js.test.utils.compiledTestOutputDirectory
import org.jetbrains.kotlin.test.backend.handlers.WasmBinaryArtifactHandler
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import java.io.File

const val WASM_BASE_FILE_NAME = "index"

abstract class AbstractWasmArtifactsCollector(testServices: TestServices) :
    WasmBinaryArtifactHandler(testServices), WasmArtifactsCollector {
    val modulesToArtifact = mutableMapOf<TestModule, BinaryArtifacts.Wasm>()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        modulesToArtifact[module] = info
    }
}

fun TestServices.getWasmTestOutputDirectory(): File {
    val originalFile = moduleStructure.originalTestDataFiles.first()
    return compiledTestOutputDirectory(
        "out",
        WasmEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR,
        WasmEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX,
        WasmEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR,
    ).resolve(originalFile.nameWithoutExtension)
}

fun TestServices.getWasmTestOutputDirectoryForMode(mode: String): File =
    getWasmTestOutputDirectory().resolve(mode)
