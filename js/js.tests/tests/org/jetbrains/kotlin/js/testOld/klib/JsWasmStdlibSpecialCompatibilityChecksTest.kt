/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.test.klib.compatibility.StdlibSpecialCompatibilityChecksTest

@Suppress("JUnitTestCaseWithNoTests")
class JsStdlibSpecialCompatibilityChecksTest : StdlibSpecialCompatibilityChecksTest, WebLibrarySpecialCompatibilityChecksTest() {
    override val isWasm: Boolean = false

    override val originalLibraryPath: String
        get() = patchedJsStdlibWithoutJarManifest

    override val libraryDisplayName: String
        get() = "standard"
}

@Suppress("JUnitTestCaseWithNoTests")
class WasmStdlibSpecialCompatibilityChecksTest : StdlibSpecialCompatibilityChecksTest, WebLibrarySpecialCompatibilityChecksTest() {
    override val isWasm: Boolean = true

    override val originalLibraryPath: String
        get() = patchedWasmStdlibWithoutJarManifest

    override val libraryDisplayName: String
        get() = "standard"
}
