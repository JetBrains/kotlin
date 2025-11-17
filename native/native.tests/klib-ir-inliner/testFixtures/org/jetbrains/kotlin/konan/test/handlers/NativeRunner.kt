/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.handlers

import org.jetbrains.kotlin.native.executors.ExecuteRequest
import org.jetbrains.kotlin.native.executors.HostExecutor
import org.jetbrains.kotlin.test.backend.handlers.NativeBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.ModuleStructureExtractor
import org.jetbrains.kotlin.test.services.TestServices

class NativeRunner(testServices: TestServices) : NativeBinaryArtifactHandler(testServices) {
    var executable: BinaryArtifacts.Native? = null
    override fun processModule(module: TestModule, info: BinaryArtifacts.Native) {
        if (module.name == ModuleStructureExtractor.DEFAULT_MODULE_NAME)
            executable = info
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        // TODO KT-82472: Perform a full run with stdout/stderr analysis
        val executeRequest = ExecuteRequest(executable?.executable?.absolutePath ?: error("Module ${ModuleStructureExtractor.DEFAULT_MODULE_NAME} is expected to run"))
        HostExecutor().execute(executeRequest)
    }
}