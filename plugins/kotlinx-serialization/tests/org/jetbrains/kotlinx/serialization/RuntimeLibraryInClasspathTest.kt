/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlinx.serialization.compiler.diagnostic.CommonVersionReader
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuntimeLibraryInClasspathTest {
    @Test
    fun testRuntimeLibraryExists() {
        assertNotNull(
            RuntimeLibraryInClasspathUtils.coreLibraryPath,
            "kotlinx-serialization runtime library is not found. Make sure it is present in test classpath",
        )
    }

    @Test
    fun testRuntimeHasSufficientVersion() {
        val version = CommonVersionReader.getVersionsFromManifest(RuntimeLibraryInClasspathUtils.coreLibraryPath!!)
        assertTrue(version.currentCompilerMatchRequired(), "Runtime version too high")
        assertTrue(version.implementationVersionMatchSupported(), "Runtime version too low")
    }
}
