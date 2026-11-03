/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.normalizedSwiftExportModuleName
import kotlin.test.Test
import kotlin.test.assertEquals

class SwiftExportModuleNameNormalizationTests {

    @Test
    fun `colons and dashes are separators`() {
        assertEquals("SharedCoreApi", ":shared:core-api".normalizedSwiftExportModuleName)
    }

    @Test
    fun `dots are separators`() {
        assertEquals("KotlinxCli", "kotlinx.cli".normalizedSwiftExportModuleName)
        assertEquals("LibCoreApi", ":lib.core:api".normalizedSwiftExportModuleName)
    }

    @Test
    fun `repeated and mixed separators collapse`() {
        assertEquals("OrgExampleMyLib", "org:example:my..lib".normalizedSwiftExportModuleName)
        assertEquals("AB", ".-a:.-b.".normalizedSwiftExportModuleName)
    }

    @Test
    fun `other characters are kept`() {
        assertEquals("Invalid!name", "invalid!name".normalizedSwiftExportModuleName)
        assertEquals("My_lib", "my_lib".normalizedSwiftExportModuleName)
    }
}
