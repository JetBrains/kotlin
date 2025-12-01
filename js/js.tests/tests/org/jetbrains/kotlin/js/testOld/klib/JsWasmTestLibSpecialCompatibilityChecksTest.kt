/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.test.services.configuration.WasmEnvironmentConfigurator

@Suppress("JUnitTestCaseWithNoTests")
class JsWasmTestLibSpecialCompatibilityChecksTest : LibrarySpecialCompatibilityChecksTest() {
    override val originalLibraryPath: String
        get() = patchedJsTestWithoutJarManifest

    override fun additionalLibraries(isWasm: Boolean): List<String> =
        if (!isWasm) listOf(patchedJsStdlibWithoutJarManifest) else listOf(WasmEnvironmentConfigurator.stdlibPath(WasmTarget.JS))

    override val libraryDisplayName: String
        get() = "kotlin-test"
}
