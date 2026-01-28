/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.handlers

import org.jetbrains.kotlin.konan.test.klib.fileCheckDump
import org.jetbrains.kotlin.konan.test.klib.fileCheckStage
import org.jetbrains.kotlin.test.backend.handlers.NativeBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure

class FileCheckHandler(testServices: TestServices) : NativeBinaryArtifactHandler(testServices) {
    private var artifact: BinaryArtifacts.Native? = null
    private var fileCheckStage: String? = null
    override fun processModule(module: TestModule, info: BinaryArtifacts.Native) {
        if (NativeEnvironmentConfigurator.isMainModule(module, testServices.moduleStructure)) {
            if (artifact != null)
                error(
                    "Internal error: more than one executable for the testcase: ${artifact!!.executable.name} and ${info.executable.name}\n" +
                            "Only one module may have no incoming dependencies"
                )
            artifact = info
            fileCheckStage = module.fileCheckStage()
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        fileCheckStage?.let { fileCheckStage ->
            val executable = artifact?.executable ?: error("One main module is expected to be in the test.")
            val fileCheckDump = executable.fileCheckDump(fileCheckStage)
            // TODO: Migrate functionality from TestRunCheck.FileCheckMatcher
        }
    }
}
