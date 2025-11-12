/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import kotlin.collections.any

@Suppress("JUnitTestCaseWithNoTests")
class JsWasmStdlibSpecialCompatibilityChecksTest : LibrarySpecialCompatibilityChecksTest() {
    override val originalLibraryPath: String
        get() = System.getProperty("kotlin.js.full.stdlib.path")

    override fun MessageCollectorImpl.hasJsOldLibraryError(specificVersions: Pair<TestVersion, TestVersion>?): Boolean {
        val stdlibMessagePart = "Kotlin/JS standard library has an older version" + specificVersions?.first?.let { " ($it)" }.orEmpty()
        val compilerMessagePart = "than the compiler" + specificVersions?.second?.let { " ($it)" }.orEmpty()

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }
    }

    override fun MessageCollectorImpl.hasJsTooNewLibraryError(specificVersions: Pair<TestVersion, TestVersion>?): Boolean {
        val stdlibMessagePart =
            "The Kotlin/JS standard library has a more recent version" + specificVersions?.first?.let { " ($it)" }.orEmpty()
        val compilerMessagePart = "The compiler version is " + specificVersions?.second?.toString().orEmpty()

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }
    }

    override fun MessageCollectorImpl.hasWasmOldLibraryError(specificVersions: Pair<TestVersion, TestVersion>?): Boolean {
        val stdlibMessagePart = "Kotlin/Wasm standard library has an older version" + specificVersions?.first?.let { " ($it)" }.orEmpty()
        val compilerMessagePart = "than the compiler" + specificVersions?.second?.let { " ($it)" }.orEmpty()

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }
    }

    override fun MessageCollectorImpl.hasWasmTooNewLibraryError(specificVersions: Pair<TestVersion, TestVersion>?): Boolean {
        val stdlibMessagePart =
            "The Kotlin/Wasm standard library has a more recent version" + specificVersions?.first?.let { " ($it)" }.orEmpty()
        val compilerMessagePart = "The compiler version is " + specificVersions?.second?.toString().orEmpty()

        return messages.any { stdlibMessagePart in it.message && compilerMessagePart in it.message }
    }
}
