/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.handlers

import org.jetbrains.kotlin.native.executors.RunProcessException
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.backend.handlers.NativeBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.NativeEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.moduleStructure
import kotlin.time.Duration.Companion.seconds

class NativeRunner(testServices: TestServices) : NativeBinaryArtifactHandler(testServices) {
    private var artifact: BinaryArtifacts.Native? = null
    override fun processModule(module: TestModule, info: BinaryArtifacts.Native) {
        if (NativeEnvironmentConfigurator.isMainModule(module, testServices.moduleStructure)) {
            if (artifact != null) error("Internal error: more that one main module in the test: $artifact and $info")
            artifact = info
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val executable = artifact?.executable
        val executableAbsolutePath = executable?.absolutePath ?: error("One main module is expected to be in the test.")
        val result = try {
            runProcess(executableAbsolutePath) {
                this.workingDirectory = executable.parentFile
                this.timeout = 10.seconds
            }
        } catch (e: RunProcessException) {
            throw AssertionError("Native executable failed with exit code ${e.exitCode}:\nstdout: ${e.stdout}\nstderr: ${e.stderr}")
        }

        if (!result.stdout.contains("[  PASSED  ] 1 tests."))
            throw AssertionError("Run failed, stdout and stderr: ${result.output}")
    }
}