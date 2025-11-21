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
import java.io.ByteArrayOutputStream

class NativeRunner(testServices: TestServices) : NativeBinaryArtifactHandler(testServices) {
    private var artifact: BinaryArtifacts.Native? = null
    override fun processModule(module: TestModule, info: BinaryArtifacts.Native) {
        if (module.name == ModuleStructureExtractor.DEFAULT_MODULE_NAME)
            artifact = info
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        // TODO KT-82472: Perform a full run with stdout/stderr analysis
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val executable = artifact?.executable
        val executableAbsolutePath = executable?.absolutePath ?: error("Module ${ModuleStructureExtractor.DEFAULT_MODULE_NAME} is expected to run")
        val request = ExecuteRequest(executableAbsolutePath).apply {
//            this.args.addAll(programArgs.drop(1))
            this.workingDirectory = executable.parentFile
//            inputStreamFromTestParameter()?.let {
//                this.stdin = it
//            }
            this.stdout = stdout
            this.stderr = stderr
//            this.timeout = testRun.checks.executionTimeoutCheck.timeout
        }
        val response = HostExecutor().execute(request)

        val stdoutStr = stdout.toString()
        if (response.exitCode != 0) {
            throw AssertionError("Native executable failed with exit code ${response.exitCode}:\nstdout: $stdoutStr\nstderr: $stderr")
        }
        if (!stdoutStr.contains("[  PASSED  ] 1 tests."))
            throw AssertionError("Run failed. stdout: $stdoutStr\nstderr: $stderr")
    }
}