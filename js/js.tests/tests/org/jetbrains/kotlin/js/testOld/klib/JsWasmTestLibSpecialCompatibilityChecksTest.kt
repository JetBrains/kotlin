/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

@Suppress("JUnitTestCaseWithNoTests")
class JsTestLibSpecialCompatibilityChecksTest : WebLibrarySpecialCompatibilityChecksTest() {
    override val isWasm = false

    override val originalLibraryPath: String
        get() = patchedJsTestWithoutJarManifest

    override fun additionalLibraries(): List<String> =
        listOf(patchedJsStdlibWithoutJarManifest)

    override val libraryDisplayName: String
        get() = "kotlin-test"
}

@Suppress("JUnitTestCaseWithNoTests")
class WasmTestLibSpecialCompatibilityChecksTest : WebLibrarySpecialCompatibilityChecksTest() {
    override val isWasm = true

    override val originalLibraryPath: String
        get() = patchedWasmTestWithoutJarManifest

    override fun additionalLibraries(): List<String> =
        listOf(patchedWasmStdlibWithoutJarManifest)

    override val libraryDisplayName: String
        get() = "kotlin-test"
}
