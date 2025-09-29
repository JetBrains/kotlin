/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.providers

import org.jetbrains.kotlin.js.test.AbstractWebJsSteppingTestAdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestServices

class WasmJsSteppingTestAdditionalSourceProvider(testServices: TestServices) : AbstractWebJsSteppingTestAdditionalSourceProvider(testServices) {
    // real sources are located inside `compiler/testData/debug/wasmTestHelpers`
    override val commonTestHelpersFile = "debugTestHelpers/wasmTestHelpers/coroutineHelpers.kt"
    override val minimalTestHelpersLocation = null
    override val withStdlibTestHelpersFile = null
}
