/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

@Suppress("JUnitTestCaseWithNoTests")
class JsWasmStdlibSpecialCompatibilityChecksTest : LibrarySpecialCompatibilityChecksTest() {
    override val originalLibraryPath: String
        get() = System.getProperty("kotlin.js.full.stdlib.path")

    override val libraryDisplayName: String
        get() = "standard"
}
