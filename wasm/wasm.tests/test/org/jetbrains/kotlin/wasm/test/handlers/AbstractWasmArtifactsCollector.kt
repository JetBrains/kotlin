/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.backend.handlers.WasmBinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractWasmArtifactsCollector(testServices: TestServices) : WasmBinaryArtifactHandler(testServices) {
    val modulesToArtifact = mutableMapOf<TestModule, BinaryArtifacts.Wasm>()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Wasm) {
        modulesToArtifact[module] = info
    }
}