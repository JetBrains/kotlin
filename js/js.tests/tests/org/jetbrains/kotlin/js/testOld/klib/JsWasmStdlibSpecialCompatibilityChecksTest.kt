/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.klib

import org.jetbrains.kotlin.js.testOld.klib.LibrarySpecialCompatibilityChecksTest.Companion.SORTED_TEST_COMPILER_VERSION_GROUPS
import org.jetbrains.kotlin.js.testOld.klib.LibrarySpecialCompatibilityChecksTest.Companion.SORTED_TEST_OLD_LIBRARY_VERSION_GROUPS

interface StdlibSpecialCompatibilityChecksTest : DummyLibraryCompiler {
    fun testExportToOlderAbiVersionWithOlderLibrary() {
        for (compilerVersion in SORTED_TEST_COMPILER_VERSION_GROUPS.flatten()) {
            for (libraryVersion in SORTED_TEST_OLD_LIBRARY_VERSION_GROUPS) {
                compileDummyLibrary(
                    libraryVersion = libraryVersion,
                    compilerVersion = compilerVersion,
                    expectedWarningStatus = WarningStatus.NO_WARNINGS,
                    exportKlibToOlderAbiVersion = true,
                )
            }
        }
    }

    fun testExportToOlderAbiVersionWithCurrentLibrary() {
        for (compilerVersion in SORTED_TEST_COMPILER_VERSION_GROUPS.flatten()) {
            for (libraryVersion in SORTED_TEST_COMPILER_VERSION_GROUPS.flatten()) {
                compileDummyLibrary(
                    libraryVersion = libraryVersion,
                    compilerVersion = compilerVersion,
                    expectedWarningStatus = WarningStatus.TOO_NEW_LIBRARY_WARNING,
                    exportKlibToOlderAbiVersion = true,
                )
            }
        }
    }
}

class JsStdlibSpecialCompatibilityChecksTest : StdlibSpecialCompatibilityChecksTest, WebLibrarySpecialCompatibilityChecksTest() {
    override val isWasm: Boolean = false

    override val originalLibraryPath: String
        get() = patchedJsStdlibWithoutJarManifest

    override val libraryDisplayName: String
        get() = "standard"

    override fun testExportToOlderAbiVersionWithCurrentLibrary() {
        super.testExportToOlderAbiVersionWithCurrentLibrary()
    }

    override fun testExportToOlderAbiVersionWithOlderLibrary() {
        super.testExportToOlderAbiVersionWithOlderLibrary()
    }
}

class WasmStdlibSpecialCompatibilityChecksTest : StdlibSpecialCompatibilityChecksTest, WebLibrarySpecialCompatibilityChecksTest() {
    override val isWasm: Boolean = true

    override val originalLibraryPath: String
        get() = patchedWasmStdlibWithoutJarManifest

    override val libraryDisplayName: String
        get() = "standard"

    override fun testExportToOlderAbiVersionWithCurrentLibrary() {
        super.testExportToOlderAbiVersionWithCurrentLibrary()
    }

    override fun testExportToOlderAbiVersionWithOlderLibrary() {
        super.testExportToOlderAbiVersionWithOlderLibrary()
    }
}
