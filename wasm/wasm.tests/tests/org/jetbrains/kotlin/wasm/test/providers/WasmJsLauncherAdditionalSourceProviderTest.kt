/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.providers

import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class WasmJsLauncherAdditionalSourceProviderTest {
    @Test
    fun `given paths with the same JVM hash then isolated launcher names remain distinct`() {
        val first = testFile("Aa")
        val second = testFile("BB")

        assertEquals(first.relativePath.hashCode(), second.relativePath.hashCode())
        assertNotEquals(
            WasmJsLauncherAdditionalSourceProvider.computeLauncherClassName(first),
            WasmJsLauncherAdditionalSourceProvider.computeLauncherClassName(second),
        )
        assertTrue(
            WasmJsLauncherAdditionalSourceProvider.computeLauncherClassName(first).matches(Regex("Launcher_[0-9a-f]+")),
        )
    }

    private fun testFile(relativePath: String): TestFile = TestFile(
        relativePath = relativePath,
        originalContent = "fun box(): String = \"OK\"",
        originalFile = File(relativePath),
        startLineNumberInOriginalFile = 0,
        isAdditional = false,
        directives = RegisteredDirectives.Empty,
    )
}
